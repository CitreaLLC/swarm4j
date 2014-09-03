package citrea.swarm4j.server;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class SwarmServer {
    public final Logger logger = LoggerFactory.getLogger(SwarmServer.class.getName());

    @Autowired
    private Utils utils;

    @Value("${port:8080}")
    private int port;

    private WSServerImpl wsServer;
    private Host host;

    public SwarmServer() {

    }

    public void setHost(Host host) {
        this.host = host;
    }

    public void stop() throws IOException, InterruptedException {
        logger.info("stopping");
        wsServer.stop();
    }

    public void start() throws UnknownHostException {
        if (host == null) {
            throw new RuntimeException("'host' property not deliver");
        }
        host.start();
        wsServer = new WSServerImpl(port, host, utils);
        wsServer.start();
        logger.info("started on port: " + port);
    }
}