package citrea.swarm4j.model;

import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.value.JSONValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 16:32
 */
public class InvalidHandshakeSwarmException extends SwarmException {
    public InvalidHandshakeSwarmException(String message) {
        super(message);
    }

    public InvalidHandshakeSwarmException(Spec spec, JSONValue value) {
        this(spec.toString() + "->" + value.toJSONString());
    }
}
