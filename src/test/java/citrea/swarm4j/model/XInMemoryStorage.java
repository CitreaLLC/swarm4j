package citrea.swarm4j.model;

import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;
import citrea.swarm4j.storage.InMemoryStorage;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 28.08.2014
 *         Time: 00:54
 */
public class XInMemoryStorage extends InMemoryStorage {

    public XInMemoryStorage(SpecToken id) {
        super(id);
    }

    @Override
    public JSONValue readState(Spec ti) {
        return super.readState(ti);
    }

    @Override
    public Map<Spec, JSONValue> readOps(Spec ti) {
        return super.readOps(ti);
    }
}
