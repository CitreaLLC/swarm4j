package citrea.swarm4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.java_websocket.WebSocketImpl;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Created with IntelliJ IDEA.
 * @author aleksisha
 * Date: 25/10/13
 * Time: 16:47
 */
@Component
public class SwarmServer implements InitializingBean {
    public final Logger logger = LogManager.getLogger(SwarmServer.class.getName());
    public final Marker markerRoot = MarkerManager.getMarker("SwarmServer");

    @Autowired
    private Utils utils;

    @Value("${port:8080}")
    private int port;

    private WSServerImpl wsServer;

    public SwarmServer() {

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        WebSocketImpl.DEBUG = true;
        start();
    }

    public void sendToAll( String text ) throws InterruptedException {
        wsServer.sendToAll(text);
    }

    public void stop() throws IOException, InterruptedException {
        logger.info(markerRoot, "stopping");
        wsServer.stop();
    }

    public void start() throws UnknownHostException {
        wsServer = new WSServerImpl(port, markerRoot, utils);
        wsServer.start();
        logger.info(markerRoot, "started on port: " + port);
    }
}