package citrea.swarm4j.model;

import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.value.JSONValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 16:34
 */
public class SelfHandshakeSwarmException extends InvalidHandshakeSwarmException {
    public SelfHandshakeSwarmException(Spec spec, JSONValue value) {
        super(spec, value);
    }
}
