package citrea.swarm4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 26/10/13
 *         Time: 01:05
 */
public class WSServerImpl extends WebSocketServer {
    public static final Logger logger = LogManager.getLogger(WSServerImpl.class);
    private final Marker markerRoot;
    private final Utils utils;

    public Map<WebSocket, SwarmPipeParams> knownPipes = new HashMap<WebSocket, SwarmPipeParams>();

    public WSServerImpl(int port, Marker markerRoot, Utils utils) throws UnknownHostException {
        super( new InetSocketAddress( port ) );
        this.markerRoot = markerRoot;
        this.utils = utils;
    }

    @Override
    public void onOpen( WebSocket conn, ClientHandshake handshake ) {
        final Marker MARK = MarkerManager.getMarker(utils.generateRandomId(6), markerRoot);
        knownPipes.put(conn, new SwarmPipeParams(MARK));
        logger.info(MARK, conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
        try {
            sendToAll( "new connection: " + handshake.getResourceDescriptor() );
        } catch (InterruptedException e) {
            logger.warn(MARK, "error sending to all err={}", e.getMessage(), e);
        }
    }

    @Override
    public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
        final Marker MARK = MarkerManager.getMarker(utils.generateRandomId(6), markerRoot);
        knownPipes.remove(conn);
        logger.info(MARK, conn + " has left the room!");
        try {
            sendToAll( conn + " has left the room!" );
        } catch (InterruptedException e) {
            logger.warn(MARK, "error sending to all err={}", e.getMessage(), e);
        }
    }

    @Override
    public void onMessage( WebSocket conn, String message ) {
        SwarmPipeParams pipeParams = knownPipes.get(conn);
        Marker MARK = pipeParams != null ? pipeParams.getLogMarker() : markerRoot;
        logger.debug(MARK, "onMessage message={}", message);
        JSONTokener jsonTokener = new JSONTokener(message);
        try {
            Object value = jsonTokener.nextValue();
            if (!(value instanceof JSONObject)) {
                //TODO better validation
                logger.warn(MARK, "onMessage operation must be a JSON message={}", message);
                return;
            }
            JSONObject op = (JSONObject) value;
            String spec = op.getString("spec");
            JSONObject val = op.getJSONObject("value");
            logger.debug(MARK, "onMessage.parsed spec={} value={}", spec, val);
        } catch (JSONException e) {
            logger.warn(MARK, "onMessage.parsing error", e);
        }
        try {
            sendToAll(message);
        } catch (InterruptedException e) {
            logger.warn(MARK, "error sending to all err={}", e.getMessage(), e);
        }
    }

    @Override
    public void onError( WebSocket conn, Exception ex ) {
        ex.printStackTrace();
        if ( conn != null ) {
            // some errors like port binding failed may not be assignable to a specific WebSocket
            knownPipes.remove(conn);
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
