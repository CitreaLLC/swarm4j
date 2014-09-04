package citrea.swarm4j.model.meta;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.spec.SpecToken;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 17:43
 */
public interface TypeMeta {
    Class<? extends Syncable> getType();
    SpecToken getTypeToken();
    Syncable newInstance(SpecToken id, Host host) throws SwarmException;

    FieldMeta getFieldMeta(String fieldName);
    Collection<FieldMeta> getAllFields();

    OperationMeta getOperationMeta(SpecToken op);
    OperationMeta getOperationMeta(String opName);

    String getDescription();
}
