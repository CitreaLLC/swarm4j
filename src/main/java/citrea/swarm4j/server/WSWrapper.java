package citrea.swarm4j.server;

import citrea.swarm4j.model.*;
import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecToken;
import citrea.swarm4j.spec.SpecWithAction;
import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONString;
import org.json.JSONStringer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 31/10/13
 *         Time: 17:58
 */
public class WSWrapper implements HandshakeAware {
    private static final Logger logger = LoggerFactory.getLogger(WSWrapper.class);

    private final Swarm swarm;

    private final WebSocket ws;
    private final String pipeId;
    private SpecToken peer;
    private boolean handshaken;
    private String clientTime;

    public WSWrapper(Swarm swarm, WebSocket ws, String pipeId) {
        this.swarm = swarm;
        this.ws = ws;
        this.pipeId = pipeId;
        this.handshaken = false;
    }

    public void sendOperation(Action action, Spec spec, JSONString value) {
        MDC.put("pipeId", this.pipeId);
        logger.debug("sendOperation action={} spec={} value={}", action, spec, value);
        SpecWithAction key = new SpecWithAction(spec, action);
        JSONStringer payload = new JSONStringer();
        try {
            payload.object();
            payload.key(key.toString());
            payload.value(value);
            payload.endObject();
        } catch (JSONException e) {
            throw new RuntimeException("error building json: " + e.getMessage(), e);
        }
        ws.send(payload.toString());
        MDC.remove("pipeId");
    }

    public String getPipeId() {
        return pipeId;
    }

    @Override
    public SpecToken getPeerId() {
        return peer;
    }

    @Override
    public void setPeerId(SpecToken peer) {
        this.peer = peer;
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

    @Override
    public void on(Action action, Spec spec, JSONValue value, EventRecipient source) throws SwarmException {
        this.sendOperation(action, spec, value);
    }

    @Override
    public void off(Action action, Spec spec, EventRecipient source) throws SwarmException {
        this.sendOperation(action, spec, null);
    }

    @Override
    public void set(Spec spec, JSONValue value, EventRecipient listener) throws SwarmException {
        this.sendOperation(Action.set, spec, value);
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
