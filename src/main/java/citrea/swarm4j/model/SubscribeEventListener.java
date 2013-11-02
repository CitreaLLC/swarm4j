package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 31/10/13
 *         Time: 18:28
 */
public interface SubscribeEventListener extends SwarmEventListener {

    void on(Spec spec, JSONValue value, SwarmEventListener source) throws SwarmException;
    void off(Spec spec, SwarmEventListener source) throws SwarmException;
}
