package citrea.swarm4j.model.callback;

import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.value.JSONValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 23.08.2014
 *         Time: 23:10
 */
public abstract class FilteringOpRecipient<T extends OpRecipient> implements OpRecipient {
    protected T inner;

    protected FilteringOpRecipient(T inner) {
        this.inner = inner;
    }

    protected abstract boolean filter(Spec spec, JSONValue value, OpRecipient source) throws SwarmException;

    protected void deliverInternal(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
        inner.deliver(spec, value, source);
    }

    @Override
    public final void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
        if (filter(spec, value, source)) {
            deliverInternal(spec, value, source);
        }
    }

    public T getInner() {
        return inner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof FilteringOpRecipient) {
            FilteringOpRecipient that = (FilteringOpRecipient) o;
            return this.inner.equals(that.inner);
        }

        return (o instanceof OpRecipient) && this.inner.equals(o);
    }

    @Override
    public int hashCode() {
        return inner.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{" +
                "inner=" + inner +
                '}';
    }
}
