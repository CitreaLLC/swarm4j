package citrea.swarm4j;

import org.apache.logging.log4j.Marker;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 25/10/13
 *         Time: 19:27
 */
public class SwarmPipeParams {
    private Marker logMarker;

    public SwarmPipeParams(Marker logMarker) {
        this.logMarker = logMarker;
    }

    public Marker getLogMarker() {
        return logMarker;
    }
}
