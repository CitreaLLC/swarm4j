package citrea.swarm4j.model;

import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03/11/13
 *         Time: 10:29
 */
public class RememberingRecipient implements EventRecipient {

    public Triplet lastOnParams;
    public Triplet lastOffParams;
    public Triplet lastSetParams;

    @Override
    public void on(Action action, Spec spec, JSONValue value, EventRecipient source) throws SwarmException {
        lastOnParams = new Triplet(spec, value, source);
    }

    @Override
    public void off(Action action, Spec spec, EventRecipient source) throws SwarmException {
        lastOffParams = new Triplet(spec, null, source);
    }

    @Override
    public void set(Spec spec, JSONValue value, EventRecipient listener) throws SwarmException {
        lastSetParams = new Triplet(spec, value, listener);
    }

    public static class Triplet {
        public final Spec spec;
        public final JSONValue value;
        public final EventRecipient listener;

        public Triplet(Spec spec, JSONValue value, EventRecipient listener) {
            this.spec = spec;
            this.value = value;
            this.listener = listener;
        }
    }
}
