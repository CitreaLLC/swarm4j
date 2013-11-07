package citrea.swarm4j.model;

import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 05/11/13
 *         Time: 19:49
 */
public class ActionSpecValueTriplet {
    private Action action;
    private Spec spec;
    private JSONValue value;

    public ActionSpecValueTriplet(Action action, Spec spec, JSONValue value) {
        this.action = action;
        this.spec = spec;
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public Spec getSpec() {
        return spec;
    }

    public JSONValue getValue() {
        return value;
    }
}
