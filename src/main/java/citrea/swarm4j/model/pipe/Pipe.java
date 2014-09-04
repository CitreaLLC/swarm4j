package citrea.swarm4j.model.pipe;

import citrea.swarm4j.model.*;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.callback.Peer;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 15:49
 */
public class Pipe implements OpStreamListener, OpRecipient, Peer {
    public static Logger logger = LoggerFactory.getLogger(Pipe.class);

    public static final Set<SpecToken> SUBSCRIPTION_OPERATIONS = new HashSet<SpecToken>(Arrays.asList(
            Syncable.ON,
            Syncable.OFF,
            Syncable.REON,
            Syncable.REOFF
    ));

    protected Host host;
    protected OpStream stream;
    protected SpecToken peerId;
    protected boolean handshaken;

    public Pipe(Host host) {
        this.host = host;
        this.handshaken = false;
    }

    @Override
    public void onMessage(String message) throws SwarmException, JSONException {
        if (logger.isDebugEnabled()) {
            logger.debug("{} <= {}: {}", host.getId(), (handshaken ? this.peerId.toString() : "?"), message);
        }
        for (Map.Entry<Spec, JSONValue> op : parse(message).entrySet()) {
            if (!handshaken) {
                this.processHandshake(op.getKey(), op.getValue());
            } else {
                this.host.deliver(op.getKey(), op.getValue(), this);
            }
        }
    }

    @Override
    public void onClose() {
        this.stream.setSink(null);
        this.stream = null;
        this.close();
    }

    @Override
    public void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
        String message = Pipe.serialize(spec, value);
        if (logger.isDebugEnabled()) {
            logger.debug("{} => {}: {}", host.getId(), (handshaken ? this.peerId.toString() : "?"), message);
        }
        this.stream.sendMessage(message);
    }

    protected void processHandshake(Spec spec, JSONValue value) throws SwarmException {
        if (spec == null) {
            throw new IllegalArgumentException("handshake has no spec");
        }
        if (!Host.HOST.equals(spec.getType())) {
            throw new InvalidHandshakeSwarmException(spec, value);
        }
        if (this.host.getId().equals(spec.getId())) {
            throw new SelfHandshakeSwarmException(spec, value);
        }
        SpecToken op = spec.getOp();
        Spec evspec = spec.overrideToken(this.host.getId()).sort();

        if (SUBSCRIPTION_OPERATIONS.contains(op)) { // access denied TODO
            this.setPeerId(spec.getId());
            this.host.deliver(evspec, value, this);
        } else {
            throw new InvalidHandshakeSwarmException(spec, value);
        }
    }

    public void setStream(OpStream stream) {
        this.stream = stream;
        this.stream.setSink(this);
    }

    public void close() {
        //TODO pipe.close()
    }

    public void setStream(URI upstreamURI) {
        //TODO pipe reconnection
        throw new UnsupportedOperationException("Not realized yet");
    }

    @Override
    public void setPeerId(SpecToken id) {
        this.peerId = id;
        this.handshaken = true;
        logger.info("{} handshaken", this.toString());
    }

    @Override
    public SpecToken getPeerId() {
        return peerId;
    }

    @Override
    public Spec getTypeId() {
        return handshaken ? new Spec(Host.HOST, peerId) : null;
    }

    @Override
    public String toString() {
        return "Pipe{ " + host.getId() +
                " <=> " + (handshaken ? peerId : "?") +
                " }";
    }

    //TODO configurable serializer
    public static String serialize(Spec spec, JSONValue value) throws SwarmException {
        JSONStringer payload = new JSONStringer();
        try {
            payload.object();
            payload.key(spec.toString());
            payload.value(value);
            payload.endObject();
        } catch (JSONException e) {
            throw new SwarmException("error building json: " + e.getMessage(), e);
        }
        return payload.toString();
    }

    public static SortedMap<Spec, JSONValue> parse(String message) throws JSONException {

        JSONTokener jsonTokener = new JSONTokener(message);
        Object bandleJSON = jsonTokener.nextValue();
        if (!(bandleJSON instanceof JSONObject)) {
            throw new IllegalArgumentException("message must be a JSON");
        }

        JSONObject bundle = (JSONObject) bandleJSON;

        // sort operations by spec
        SortedMap<Spec, JSONValue> operations = new TreeMap<Spec, JSONValue>(Spec.ORDER_NATURAL);
        Iterator it = bundle.keys();
        while (it.hasNext()) {
            final String specStr = String.valueOf(it.next());
            final Spec spec = new Spec(specStr);
            final JSONValue value = JSONValue.convert(bundle.get(specStr));
            operations.put(spec, value);
        }
        return operations;
    }

}
