package citrea.swarm4j.model.callback;

import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 18.08.2014
 *         Time: 16:58
 */
public class OpFilterRecipient extends FilteringOpRecipient<OpRecipient> {

    private SpecToken op;

    public OpFilterRecipient(OpRecipient inner, SpecToken op) {
        super(inner);
        this.op = op;
    }

    @Override
    public boolean filter(Spec spec, JSONValue value, OpRecipient source) {
        return op.equals(spec.getOp());
    }

    public SpecToken getOp() {
        return op;
    }

}
