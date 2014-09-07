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

    protected PipeState state;
    protected Host host;
    protected OpStream stream;
    protected SpecToken peerId;
    protected Boolean isOnSent = null;

    public Pipe(Host host) {
        this.host = host;
        this.state = PipeState.NEW;
    }

    @Override
    public void onMessage(String message) throws SwarmException, JSONException {
        if (logger.isDebugEnabled()) {
            logger.debug("{} <= {}: {}", host.getId(), (!PipeState.NEW.equals(state) ? this.peerId.toString() : "?"), message);
        }
        for (Map.Entry<Spec, JSONValue> op : parse(message).entrySet()) {
            switch (state) {
                case NEW:
                    processHandshake(op.getKey(), op.getValue());
                    break;
                case HANDSHAKEN:
                    host.deliver(op.getKey(), op.getValue(), this);
                    break;
                case CLOSED:
                    logger.warn("{}.onMessage() but pipe closed", this);
                    break;
            }
        }
    }

    @Override
    public void onClose() {
        this.stream.setSink(null);
        this.stream = null;
        this.close(null);
    }

    @Override
    public void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
        String message = Pipe.serialize(spec, value);
        if (logger.isDebugEnabled()) {
            logger.debug("{} => {}: {}", host.getId(), (!PipeState.NEW.equals(state) ? this.peerId.toString() : "?"), message);
        }
        if (this.stream == null) {
            return;
        }

        this.stream.sendMessage(message);

        if (Host.HOST.equals(spec.getType())) {
            final SpecToken op = spec.getOp();
            if (Syncable.ON.equals(op)) {
                this.isOnSent = true;
            } else if (Syncable.REON.equals(op)) {
                this.isOnSent = false;
            } else if (Syncable.OFF.equals(op)) {
                this.close(null);
            } else if (Syncable.REOFF.equals(op)) {
                this.isOnSent = null;
            }
        }
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

    public void close(String error) {
        logger.info("{} closing {}", this, error != null ? error : "correct");
        /* TODO pipe reconnection
        if (error != null && this.host != null && this.url) {
            var uplink_uri = this.url,
                    host = this.host,
                    pipe_opts = this.opts;
            //reconnect delay for next disconnection
            pipe_opts.reconnectDelay = Math.min(30000, this.reconnectDelay << 1);
            // schedule a retry
            setTimeout(function () {
                host.connect(uplink_uri, pipe_opts);
            }, this.reconnectDelay);

            this.url = null; //to prevent second reconnection timer
        }
        */
        if (PipeState.HANDSHAKEN.equals(state)) {
            if (this.isOnSent != null) {
                // emulate normal off
                Spec offspec = this.host.newEventSpec(this.isOnSent ? Syncable.OFF : Syncable.REOFF);
                try {
                    this.host.deliver(offspec, JSONValue.NULL, this);
                } catch (SwarmException e) {
                    logger.warn("{}.close(): Error delivering {} to host", this, offspec);
                }
            }
            this.state = PipeState.CLOSED; // can't pass any more messages
        }
        /* TODO pipe reconnection
        if (this.katimer) {
            clearInterval(this.katimer);
            this.katimer = null;
        }
        */
        if (this.stream != null) {
            try {
                this.stream.close();
            } catch (Exception e) {
                // ignore
            }
            this.stream = null;
        }
    }

    public void setStream(URI upstreamURI) {
        //TODO pipe reconnection
        throw new UnsupportedOperationException("Not realized yet");
    }

    @Override
    public void setPeerId(SpecToken id) {
        this.peerId = id;
        this.state = PipeState.HANDSHAKEN;
        logger.info("{} handshaken", this);
    }

    @Override
    public SpecToken getPeerId() {
        return peerId;
    }

    private Spec typeId = null;

    @Override
    public Spec getTypeId() {
        if (typeId == null && PipeState.HANDSHAKEN.equals(state)) {
            typeId = new Spec(Host.HOST, peerId);
        }
        return typeId;
    }

    @Override
    public String toString() {
        return "Pipe{ " + host.getId() +
                " <=> " + (!PipeState.NEW.equals(state) ? peerId : "?") +
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
