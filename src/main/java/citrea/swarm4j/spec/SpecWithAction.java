package citrea.swarm4j.spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03/11/13
 *         Time: 12:34
 */
public class SpecWithAction {

    private final Spec spec;
    private final Action action;

    public SpecWithAction(String specWithActionAsStr) {
        int pos = specWithActionAsStr.indexOf("*");
        if (pos > -1) {
            this.spec = new Spec(specWithActionAsStr.substring(0, pos));
            this.action = Action.valueOf(specWithActionAsStr.substring(pos + 1));
        } else {
            this.spec = new Spec(specWithActionAsStr);
            this.action = Action.set;
        }
    }

    public SpecWithAction(Spec spec, Action action) {
        this.spec = spec;
        this.action = action;
    }

    public Spec getSpec() {
        return spec;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public String toString() {
        return spec.toString() + action.asToken();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpecWithAction that = (SpecWithAction) o;

        return action == that.action && spec.equals(that.spec);

    }

    @Override
    public int hashCode() {
        int result = spec.hashCode();
        result = 31 * result + action.hashCode();
        return result;
    }
}
