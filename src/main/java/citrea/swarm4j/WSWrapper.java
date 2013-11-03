package citrea.swarm4j;

import citrea.swarm4j.model.*;
import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecToken;
import citrea.swarm4j.spec.SpecWithAction;
import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONString;
import org.json.JSONStringer;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 31/10/13
 *         Time: 17:58
 */
public class WSWrapper implements HandshakeRecipient {

    public static enum State {
        NEW, KNOW_PEER, HANDSHAKEN
    }

    private final Swarm swarm;

    private final WebSocket ws;
    private final String pipeId;
    private SpecToken peer;
    private State state;
    private String clientTime;

    public WSWrapper(Swarm swarm, WebSocket ws, String pipeId) {
        this.swarm = swarm;
        this.ws = ws;
        this.pipeId = pipeId;
        this.state = State.NEW;
    }

    public void sendOperation(Action action, Spec spec, JSONString value) {
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
    }

    public SpecToken getPeer() {
        return peer;
    }

    @Override
    public void setPeerId(SpecToken peer) {
        this.peer = peer;
        this.state = State.KNOW_PEER;
    }

    public void markAsHandshaken() {
        this.state = State.HANDSHAKEN;
    }

    public State getState() {
        return this.state;
    }

    @Override
    public void setClientTs(String clientTime) {
        this.clientTime = clientTime;
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
}
