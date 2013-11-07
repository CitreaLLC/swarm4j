package citrea.swarm4j.server;

import citrea.swarm4j.Utils;
import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.model.JSONValue;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.spec.SpecWithAction;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final Logger logger = LoggerFactory.getLogger(WSServerImpl.class);
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
        Thread.currentThread().setName("ws-" + wsId);
        knownPipes.put(conn, new WSWrapper(swarm, conn, wsId));
        logger.info("pipeOpen");
    }

    @Override
    public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
        WSWrapper ws = knownPipes.get(conn);
        if (ws != null) {
            try {
                ws.close();
            } catch (SwarmException e) {
                logger.warn("pipeClose err={}", e.getMessage(), e);
            }
        }
        knownPipes.remove(conn);
        logger.info("pipeClose");
    }

    @Override
    public void onError( WebSocket conn, Exception ex ) {
        logger.warn("onError error={}", ex.getMessage(), ex);
    }

    @Override
    public void onMessage( WebSocket conn, String message ) {
        WSWrapper ws = knownPipes.get(conn);
        logger.debug("onMessage.start msg={}", message);
        JSONTokener jsonTokener = new JSONTokener(message);
        try {
            Object operation = jsonTokener.nextValue();
            if (!(operation instanceof JSONObject)) {
                logger.warn("onMessage message must be a JSON");
                return;
            }

            JSONObject op = (JSONObject) operation;
            Iterator it = op.keys();

            synchronized (swarm) {

                while (it.hasNext()) {
                    final String specActionStr = String.valueOf(it.next());
                    final JSONValue value = new JSONValue(op.get(specActionStr));
                    final SpecWithAction specWithAction = new SpecWithAction(specActionStr);
                    final Action action = specWithAction.getAction();
                    final Spec spec = specWithAction.getSpec();
                    try {
                        logger.debug("onMessage.parsed action={} spec={} value={}", action, spec, value.toJSONString());
                        swarm.deliver(action, spec, value, ws);
                    } catch (SwarmException e) {
                        logger.warn("onMessage err={}", e.getMessage(), e);
                        ws.sendOperation(Action.err, spec, new JSONValue(e.getMessage()));
                    }
                }

            }

        } catch (JSONException e) {
            //send error
            ws.sendOperation(Action.err, swarm.getSpec(), new JSONValue("error parsing or generating JSON: " + e.getMessage()));
        }
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
