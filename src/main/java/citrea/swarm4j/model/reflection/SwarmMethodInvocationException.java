package citrea.swarm4j.model.reflection;

import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 22.08.2014
 *         Time: 16:10
 */
public class SwarmMethodInvocationException extends SwarmException {

    public SwarmMethodInvocationException(String message) {
        super(message);
    }

    public SwarmMethodInvocationException(String message, Throwable inner) {
        super(message, inner);
    }
}
