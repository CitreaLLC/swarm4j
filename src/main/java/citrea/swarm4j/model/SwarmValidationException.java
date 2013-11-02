package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02/11/13
 *         Time: 23:08
 */
public class SwarmValidationException extends SwarmException {
    private Spec spec;

    public SwarmValidationException(Spec spec, String msg) {
        super("Validation error: " + msg + " (" + spec.toString() + ")");
        this.spec = spec;
    }

    public Spec getSpec() {
        return spec;
    }
}
