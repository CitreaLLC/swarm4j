package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecQuant;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 31/10/13
 *         Time: 17:33
 */
public class SwarmNoTokenException extends SwarmException {
    private Spec spec;
    private SpecQuant quant;

    public SwarmNoTokenException(Spec spec, SpecQuant childKey) {
        super("No '" + childKey.toString() + "' token in spec: '" + spec.toString() + "'");
        this.spec = spec;
        this.quant = quant;
    }

    public Spec getSpec() {
        return spec;
    }

    public SpecQuant getQuant() {
        return quant;
    }
}
