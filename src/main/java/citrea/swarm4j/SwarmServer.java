package citrea.swarm4j;

import citrea.swarm4j.model.Swarm;
import citrea.swarm4j.spec.SpecToken;
import org.java_websocket.WebSocketImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * @author aleksisha
 * Date: 25/10/13
 * Time: 16:47
 */
@Component
public class SwarmServer {
    public final Logger logger = LoggerFactory.getLogger(SwarmServer.class.getName());

    @Autowired
    private Utils utils;

    @Value("${port:8080}")
    private int port;

    private WSServerImpl wsServer;
    private Swarm swarm;

    public SwarmServer() {

    }

    public void setSwarm(Swarm swarm) {
        this.swarm = swarm;
    }

    public void sendToAll( String text ) throws InterruptedException {
        wsServer.sendToAll(text);
    }

    public void stop() throws IOException, InterruptedException {
        logger.info("stopping");
        wsServer.stop();
    }

    public void start() throws UnknownHostException {
        if (swarm == null) {
            throw new RuntimeException("'swarm' property not set");
        }
        wsServer = new WSServerImpl(port, swarm, utils);
        wsServer.start();
        logger.info("started on port: " + port);
    }
}