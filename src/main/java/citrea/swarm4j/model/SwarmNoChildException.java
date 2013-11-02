package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 31/10/13
 *         Time: 17:29
 */
public class SwarmNoChildException extends SwarmException{

    private Spec spec;

    public SwarmNoChildException(Spec spec) {
        super("destination not found: " + spec.toString());
        this.spec = spec;
    }

    public Spec getSpec() {
        return spec;
    }
}
