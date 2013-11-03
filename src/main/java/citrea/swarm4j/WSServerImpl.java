package citrea.swarm4j;

import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecToken;
import citrea.swarm4j.model.JSONValue;
import citrea.swarm4j.model.Swarm;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.spec.SpecWithAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.*;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 26/10/13
 *         Time: 01:05
 */
public class WSServerImpl extends WebSocketServer {
    public static final Logger logger = LogManager.getLogger(WSServerImpl.class);
    private final Utils utils;

    private Swarm swarm;
    private Map<WebSocket, WSWrapper> knownPipes = new HashMap<WebSocket, WSWrapper>();

    public WSServerImpl(int port, Swarm swarm, Utils utils) throws UnknownHostException {
        super( new InetSocketAddress( port ) );
        this.utils = utils;
        this.swarm = swarm;
    }

    @Override
    public void onOpen( WebSocket conn, ClientHandshake handshake ) {
        final String wsId = utils.generateRandomId(6);
        knownPipes.put(conn, new WSWrapper(swarm, conn, wsId));
        logger.info("connected entered the room!");
        try {
            sendToAll( "new connection: " + handshake.getResourceDescriptor() );
        } catch (InterruptedException e) {
            logger.warn("error sending to all err={}", e.getMessage(), e);
        }
    }

    @Override
    public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
        WSWrapper ws = knownPipes.get(conn);
        if (ws != null) {
            try {
                ws.close();
            } catch (SwarmException e) {
                logger.warn("error closing ws-wrapper err={}", e.getMessage(), e);
            }
        }
        knownPipes.remove(conn);
        logger.info(ws + " has left the room!");
    }

    @Override
    public void onError( WebSocket conn, Exception ex ) {
        logger.warn("onError error={}", ex.getMessage(), ex);
    }

    @Override
    public void onMessage( WebSocket conn, String message ) {
        WSWrapper ws = knownPipes.get(conn);
        logger.entry(message);
        JSONTokener jsonTokener = new JSONTokener(message);
        try {
            Object operation = jsonTokener.nextValue();
            if (!(operation instanceof JSONObject)) {
                logger.warn("onMessage message must be a JSON");
                return;
            }

            JSONObject op = (JSONObject) operation;
            Iterator it = op.keys();
            while (it.hasNext()) {
                final String specActionStr = String.valueOf(it.next());
                final JSONValue value = new JSONValue(op.get(specActionStr));
                final SpecWithAction specWithAction = new SpecWithAction(specActionStr);
                final Action action = specWithAction.getAction();
                final Spec spec = specWithAction.getSpec();
                logger.debug("action={} spec={} value={}", action, spec, value.toJSONString());
                swarm.deliver(action, spec, value, ws);
            }
        } catch (JSONException e) {
            //TODO send error
            logger.warn("onMessage.parsing error", e);
        } catch (SwarmException e) {
            logger.warn("onMessage swarmException ", e);
        }
        logger.exit();
    }

    /**
     * Sends <var>message</var> to all currently connected WebSocket clients.
     *
     * @param message
     *            The String to send across the network.
     * @throws InterruptedException
     *             When socket related I/O errors occur.
     */
    public void sendToAll( String message ) throws InterruptedException {
        Collection<WebSocket> con = connections();
        synchronized ( con ) {
            for( WebSocket c : con ) {
                c.send( message );
            }
        }
    }
}
