package citrea.swarm4j.model.meta;

import citrea.swarm4j.model.value.JSONValue;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 19:46
 */
public interface FieldMeta {
    String getName();
    JSONValue get(Syncable object) throws SwarmException;
    void set(Syncable object, JSONValue value) throws SwarmException;
}
