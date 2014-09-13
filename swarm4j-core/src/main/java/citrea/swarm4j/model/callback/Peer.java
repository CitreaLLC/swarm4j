package citrea.swarm4j.model.callback;

import citrea.swarm4j.model.ReferringToPeer;
import citrea.swarm4j.model.SomeSyncable;
import citrea.swarm4j.model.spec.SpecToken;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 21.06.2014
 *         Time: 16:33
 */
public interface Peer extends Uplink, ReferringToPeer {

    void setPeerId(SpecToken id);
}
