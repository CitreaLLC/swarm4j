package citrea.swarm4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * SwarmServerApplication
 *
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 25/10/13
 *         Time: 20:38
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SwarmServerApp implements CommandLineRunner {

    public static final Logger logger = LogManager.getLogger(SwarmServerApp.class);

    @Autowired
    private SwarmServer swarmServer;

    public static void main(String[] args) {
        SpringApplication.run(SwarmServerApp.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
        while ( true ) {
            try {
                String cmd = in.readLine();
                swarmServer.sendToAll(cmd);
                if( cmd.equals( "exit" ) ) {
                    swarmServer.stop();
                    break;
                } else if (cmd.equals("restart")) {
                    swarmServer.stop();
                    swarmServer.start();
                }
            } catch (InterruptedException e) {
                logger.warn(e);
                break;
            } catch (NullPointerException e) {
                System.exit(0);
                break;
            }
        }
    }
}
