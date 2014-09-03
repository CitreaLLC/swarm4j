package citrea.swarm4j.server;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.pipe.Pipe;
import citrea.swarm4j.model.spec.SpecQuant;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.util.Utils;
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
        WSWrapper stream = new WSWrapper(conn, wsId);
        host.accept(stream);
        knownPipes.put(conn, stream);
        logger.info("pipeOpen");
    }

    @Override
    public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
        WSWrapper ws = knownPipes.get(conn);
        if (ws != null) {
            ws.close(); // TODO stream.close
        }
        knownPipes.remove(conn);
        logger.info("pipeClose");
    }

    @Override
    public void onError( WebSocket conn, Exception ex ) {
        WSWrapper ws = knownPipes.get(conn);
        if (ws != null) {
            ws.close(); // TODO stream.close
        }
        knownPipes.remove(conn);
        logger.warn("pipeError error={}", ex.getMessage(), ex);
    }

    @Override
    public void onMessage( WebSocket conn, String message ) {
        WSWrapper ws = knownPipes.get(conn);
        if (ws == null) {
            logger.warn("Unknown WebSocket: {}", conn.toString());
            return;
        }
        try {
            ws.processMessage(message);
        } catch (Exception e) {
            //send error
            try {
                ws.sendMessage(
                        Pipe.serialize(
                                host.newEventSpec(Syncable.ERROR),
                                new JSONValue("error parsing or generating JSON: " + e.getMessage())
                        )
                );
            } catch (SwarmException e1) {
                //ignore
            }
            logger.warn("onMessage error errMessage={}", e.getMessage(), e);
        }
    }
}
