package citrea.swarm4j.model.callback;

import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.value.JSONValue;

import java.util.EventListener;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 29/10/13
 *         Time: 01:49
 */
public interface OpRecipient extends EventListener {

    public static OpRecipient NOOP = new OpRecipient() {
        @Override
        public void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
            //do nothing
        }
    };

    void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException;
}
