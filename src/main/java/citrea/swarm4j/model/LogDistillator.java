package citrea.swarm4j.model;

import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.value.JSONValue;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 05.09.2014
 *         Time: 01:03
 */
public interface LogDistillator {
    Map<String, JSONValue> distillLog(Map<Spec, JSONValue> oplog);
}
