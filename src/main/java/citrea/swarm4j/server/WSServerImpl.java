package citrea.swarm4j.server;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.util.Utils;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.value.JSONValue;
import citrea.swarm4j.model.SwarmException;
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

    private final Host host;
    private Map<WebSocket, WSWrapper> knownPipes = new HashMap<WebSocket, WSWrapper>();

    public WSServerImpl(int port, Host host, Utils utils) throws UnknownHostException {
        super( new InetSocketAddress( port ) );
        this.utils = utils;
        this.host = host;
    }

    @Override
    public void onOpen( WebSocket conn, ClientHandshake handshake ) {
        final String wsId = utils.generateRandomId(6);
        Thread.currentThread().setName("ws-" + wsId);
        knownPipes.put(conn, new WSWrapper(conn, wsId, host));
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
        JSONTokener jsonTokener = new JSONTokener(message);
        try {
            logger.debug("onMessage.start msg={}", message);
            Object bandleJSON = jsonTokener.nextValue();
            if (!(bandleJSON instanceof JSONObject)) {
                logger.warn("onMessage message must be a JSON");
                return;
            }

            JSONObject bandle = (JSONObject) bandleJSON;
            if (!ws.isHandshaken()) {
                ws.parseHandshake(bandle);
            } else {
                ws.parseBundle(bandle);
            }

        } catch (JSONException e) {
            //send error
            ws.sendOperation(host.getTypeId(), new JSONValue("error parsing or generating JSON: " + e.getMessage()));
        } catch (SwarmException e) {
            //send error
            ws.sendOperation(host.getTypeId(), new JSONValue("Error: " + e.getMessage()));
            logger.warn("onMessage error message={}", e.getMessage(), e);
        }
    }
}
