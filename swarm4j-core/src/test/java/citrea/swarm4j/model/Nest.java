package citrea.swarm4j.model;

import citrea.swarm4j.model.spec.SpecToken;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 05.09.2014
 *         Time: 01:56
 */
public class Nest extends Set<Duck> {

    public Nest(SpecToken id, Host host) throws SwarmException {
        super(id, host);
    }

    public Nest(Host host) throws SwarmException {
        this(null, host);
    }
}
