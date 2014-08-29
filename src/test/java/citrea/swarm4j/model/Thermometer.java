package citrea.swarm4j.model;

import citrea.swarm4j.model.annotation.SwarmField;
import citrea.swarm4j.model.annotation.SwarmType;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 28.08.2014
 *         Time: 22:49
 */
@SwarmType()
public class Thermometer extends Model {

    @SwarmField()
    public int t;


    public Thermometer(SpecToken id, Host host) throws SwarmException {
        super(id, host);
    }

    public Thermometer(JSONValue initialState, Host host) throws SwarmException {
        super(initialState, host);
    }
}
