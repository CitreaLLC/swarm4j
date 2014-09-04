package citrea.swarm4j.model.callback;

import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.value.JSONValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 22.06.2014
 *         Time: 00:59
 */
public class OnceOpRecipient extends FilteringOpRecipient<OpRecipient> {

    private Syncable obj;

    public OnceOpRecipient(Syncable obj, OpRecipient inner) {
        super(inner);
        this.obj = obj;
    }

    @Override
    protected boolean filter(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
        return true;
    }

    @Override
    protected void deliverInternal(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
        super.deliverInternal(spec, value, source);
        obj.off(this);
    }

    @Override
    public String toString() {
        return "OnceOpRecipient{" +
                "objTypeId=" + obj.getTypeId() +
                '}';
    }
}
