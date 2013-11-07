package citrea.swarm4j.server;

import citrea.swarm4j.model.EventRecipient;
import citrea.swarm4j.model.JSONValue;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 06/11/13
 *         Time: 20:23
 */
public abstract class StorageWrapper implements EventRecipient {

    @Override
    public void on(Action action, Spec spec, JSONValue value, EventRecipient source) throws SwarmException {
        //TODO load object
    }

    @Override
    public void off(Action action, Spec spec, EventRecipient source) throws SwarmException {
        //TODO unload object?
    }

    @Override
    public void set(Spec spec, JSONValue value, EventRecipient source) throws SwarmException {
        //TODO update object
    }
}
