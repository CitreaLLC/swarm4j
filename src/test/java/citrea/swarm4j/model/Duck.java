package citrea.swarm4j.model;

import citrea.swarm4j.model.annotation.SwarmField;
import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.annotation.SwarmType;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;
import org.json.JSONException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 26.08.2014
 *         Time: 21:39
 */
@SwarmType("Duck")
public class Duck extends Model {

    public static final SpecToken GROW = new SpecToken(".grow");

    @SwarmField()
    public Integer age;

    @SwarmField()
    public Integer height;

    @SwarmField()
    public String mood = "neutral";

    public Duck(Host host2) throws SwarmException {
        super((SpecToken) null, host2);
    }

    public Duck(SpecToken id, Host host) throws SwarmException {
        super(id, host);
    }

    public Duck(JSONValue initialState, Host host2) throws SwarmException {
        super(initialState, host2);
    }

    // Simply a regular convenience method
    public boolean canDrink() {
        return this.age >= 18; // Russia
    }

    public String validate(Spec spec, JSONValue value) {
        return ""; // :|
        //return spec.op()!=='set' || !('height' in val);
        //throw new Error("can't set height, may only grow");
    }

    @SwarmOperation(kind = SwarmOperationKind.Logged)
    public void grow(Spec spec, JSONValue by, OpRecipient source) {
        Integer byAsInt = by.getValueAsInteger();
        if (byAsInt != null) {
            this.height += byAsInt;
        }
    }

    // should be generated
    public void grow(int by) throws JSONException, SwarmException {
        Spec growSpec = this.newEventSpec(GROW);
        this.deliver(growSpec, new JSONValue(by), OpRecipient.NOOP);
    }
}
