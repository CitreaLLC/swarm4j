package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;

import java.util.EventListener;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 29/10/13
 *         Time: 01:49
 */
public interface SwarmEventListener extends EventListener {

    void set(Spec spec, JSONValue value, SwarmEventListener listener) throws SwarmException;
}
