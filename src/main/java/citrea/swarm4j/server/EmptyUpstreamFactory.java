package citrea.swarm4j.server;

import citrea.swarm4j.model.EventRecipient;
import citrea.swarm4j.model.JSONValue;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.SwarmUnsupportedActionException;
import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 07/11/13
 *         Time: 18:47
 */
public class EmptyUpstreamFactory implements UpstreamFactory {
    private static final Logger logger = LoggerFactory.getLogger(EmptyUpstreamFactory.class);

    private EmptyStorage storage = new EmptyStorage();

    @Override
    public void onPeerConnected(EventRecipient peer) {
    }

    @Override
    public void onPeerDisconnected(EventRecipient peer) {
    }

    @Override
    public EventRecipient getUpstream(Spec spec) {
        logger.trace("getUpstream spec={}", spec);
        return storage;
    }

    public static class EmptyStorage implements EventRecipient {
        public static final Logger logger = LoggerFactory.getLogger(EmptyStorage.class);

        @Override
        public void on(Action action, Spec spec, JSONValue value, EventRecipient source) throws SwarmException {
            logger.trace("on action={} spec={} value={}", action, spec, value);
            //imitate object loaded
            source.set(spec, null, this);
        }

        @Override
        public void off(Action action, Spec spec, EventRecipient source) throws SwarmException {
            throw new SwarmUnsupportedActionException(action);
        }

        @Override
        public void set(Spec spec, JSONValue value, EventRecipient source) throws SwarmException {
            logger.trace("set spec={} value={}", spec, value);
        }
    }
}
