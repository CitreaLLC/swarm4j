package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 31/10/13
 *         Time: 01:17
 */
public interface SubscribeReplyListener extends SwarmEventListener {
    void reOn(Spec spec, JSONValue value);
}
