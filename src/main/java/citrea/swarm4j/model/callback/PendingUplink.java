package citrea.swarm4j.model.callback;

import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 22.06.2014
 *         Time: 00:33
 */
public class PendingUplink extends FilteringOpRecipient<Uplink> implements Uplink {

    public PendingUplink(Uplink original) {
        super(original);
    }

    @Override
    protected boolean filter(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
        return false; // skip all operations
    }

    @Override
    public Spec getTypeId() {
        return this.inner.getTypeId();
    }
}
