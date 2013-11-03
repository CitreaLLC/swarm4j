package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;
import org.json.JSONString;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 29/10/13
 *         Time: 14:53
 */
public interface SwarmValidator {

    public boolean validate(Spec spec, JSONString value, EventRecipient listener);
}
