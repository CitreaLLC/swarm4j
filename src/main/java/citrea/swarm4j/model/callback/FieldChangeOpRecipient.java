package citrea.swarm4j.model.callback;

import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.value.JSONValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 23.08.2014
 *         Time: 22:59
 */
public class FieldChangeOpRecipient extends FilteringOpRecipient<OpRecipient> {

    private String fieldName;

    public FieldChangeOpRecipient(OpRecipient inner, String fieldName) {
        super(inner);
        this.fieldName = fieldName;
    }

    @Override
    public boolean filter(Spec spec, JSONValue value, OpRecipient source) {
        return value.getFieldNames().contains(this.fieldName);
    }

    @Override
    public String toString() {
        return "FieldChangeOpRecipient{" +
                "fieldName='" + fieldName + "\', " +
                "inner=" + inner +
                '}';
    }
}
