package citrea.swarm4j.server;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.callback.Peer;
import citrea.swarm4j.model.*;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;
import com.sun.corba.se.impl.orbutil.concurrent.Sync;
import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.json.JSONStringer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 31/10/13
 *         Time: 17:58
 */
public class WSWrapper implements Peer, HandshakeAware {
    private static final Logger logger = LoggerFactory.getLogger(WSWrapper.class);
    public static final Set<SpecToken> SUBSCRIPTION_OPERATIONS = new HashSet<SpecToken>(Arrays.asList(
            Syncable.ON,
            Syncable.OFF,
            Syncable.REON,
            Syncable.REOFF
    ));
    private final Host host;
    private final WebSocket ws;
    private final String pipeId;
    private Spec peerId;
    private boolean handshaken;
    private String clientTime;

    public WSWrapper(WebSocket ws, String pipeId, Host host) {
        this.ws = ws;
        this.pipeId = pipeId;
        this.host = host;
        this.handshaken = false;
    }

    public void sendOperation(Spec spec, JSONString value) {
        MDC.put("pipeId", this.pipeId);
        logger.debug("sendOperation spec={} value={}", spec, value);
        JSONStringer payload = new JSONStringer();
        try {
            payload.object();
            payload.key(spec.toString());
            payload.value(value);
            payload.endObject();
        } catch (JSONException e) {
            throw new RuntimeException("error building json: " + e.getMessage(), e);
        }
        ws.send(payload.toString());
        MDC.remove("pipeId");
    }


    @Override
    public Spec getTypeId() {
        return peerId;
    }

    @Override
    public Spec getPeerId() {
        return getTypeId();
    }

    @Override
    public void setPeerId(SpecToken peerId) {
        this.peerId = new Spec(Host.HOST, peerId);
        this.handshaken = true;
    }

    @Override
    public void setClientTs(String clientTime) {
        this.clientTime = clientTime;
    }

    @Override
    public boolean isHandshaken() {
        return handshaken;
    }

    public void parseHandshake(JSONObject handshake) throws JSONException, SwarmException {
        // sort operations by spec
        SortedMap<Spec, JSONValue> operations = new TreeMap<Spec, JSONValue>(Spec.ORDER_NATURAL);
        final Spec spec;
        final JSONValue value;
        Iterator it = handshake.keys();
        if (it.hasNext()) {
            final String specStr = String.valueOf(it.next());
            spec = new Spec(specStr);
            value = new JSONValue(handshake.get(specStr));
        } else {
            spec = null;
            value = null;
        }

        if (spec == null) {
            throw new IllegalArgumentException("handshake has no spec");
        }
        if (!Host.HOST.equals(spec.getType())) {
            logger.warn("non-Host handshake: spec={} value={}", spec, value.toJSONString());
        }
        if (this.host.getId().equals(spec.getId())) {
            throw new IllegalArgumentException("self handshake");
        }
        SpecToken op = spec.getOp();
        Spec evspec = spec.overrideToken(this.host.getId()).sort();


        if (SUBSCRIPTION_OPERATIONS.contains(op)) { // access denied TODO
            this.setPeerId(spec.getId());
            this.host.deliver(evspec, value, this);
        } else {
            throw new IllegalArgumentException("invalid handshake");
        }
    }

    public void parseBundle(JSONObject bandle) throws JSONException {
        // sort operations by spec
        SortedMap<Spec, JSONValue> operations = new TreeMap<Spec, JSONValue>(Spec.ORDER_NATURAL);
        Iterator it = bandle.keys();
        while (it.hasNext()) {
            final String specStr = String.valueOf(it.next());
            final Spec spec = new Spec(specStr);
            final JSONValue value = new JSONValue(bandle.get(specStr));
            operations.put(spec, value);
        }

        // apply operations
        synchronized (host) {
            for (Map.Entry<Spec, JSONValue> op : operations.entrySet()) {
                Spec spec = op.getKey();
                JSONValue value = op.getValue();
                try {
                    logger.debug("onMessage.parsed spec={} value={}", spec, value.toJSONString());
                    host.deliver(spec, value, this);
                } catch (SwarmException e) {
                    logger.warn("onMessage err={}", e.getMessage(), e);
                    this.sendOperation(spec.overrideToken(Syncable.ERROR), new JSONValue(e.getMessage()));
                }
            }
        }

    }
    @Override
    public void deliver(Spec spec, JSONValue value, OpRecipient listener) throws SwarmException {
        this.sendOperation(spec, value);
    }

    public void close() throws SwarmException {
        //TODO off all subscriptions
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WSWrapper wsWrapper = (WSWrapper) o;

        return ws.equals(wsWrapper.ws);
    }

    @Override
    public int hashCode() {
        return ws.hashCode();
    }
}
