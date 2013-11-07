package citrea.swarm4j.model;

import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;

import java.util.EventListener;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 29/10/13
 *         Time: 01:49
 */
public interface EventRecipient extends EventListener {

    void on(Action action, Spec spec, JSONValue value, EventRecipient source) throws SwarmException;
    void off(Action action, Spec spec, EventRecipient source) throws SwarmException;
    void set(Spec spec, JSONValue value, EventRecipient source) throws SwarmException;
}
