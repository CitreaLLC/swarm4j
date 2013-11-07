package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecQuant;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 31/10/13
 *         Time: 17:29
 */
public class SwarmNoChildException extends SwarmException{

    private final SpecQuant q;
    private final Spec spec;

    public SwarmNoChildException(Spec spec, SpecQuant quant) {
        super("destination '" + quant.name() + "' not found: " + spec.toString());
        this.spec = spec;
        this.q = quant;
    }

    public Spec getSpec() {
        return spec;
    }

    public SpecQuant getQuant() {
        return q;
    }
}
