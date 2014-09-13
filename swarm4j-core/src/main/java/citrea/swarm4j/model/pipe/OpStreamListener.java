package citrea.swarm4j.model.pipe;

import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.value.JSONValue;
import org.json.JSONException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 15:49
 */
public interface OpStreamListener {

    public void onMessage(String message) throws SwarmException, JSONException;
    public void onClose();
}
