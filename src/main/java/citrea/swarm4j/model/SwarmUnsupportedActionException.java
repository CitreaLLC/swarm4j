package citrea.swarm4j.model;

import citrea.swarm4j.spec.Action;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03/11/13
 *         Time: 15:57
 */
public class SwarmUnsupportedActionException extends SwarmException {
    public SwarmUnsupportedActionException(Action action) {
        super("'" + action + "' is unsupported");
    }
}
