package citrea.swarm4j.server;

import citrea.swarm4j.model.EventRecipient;
import citrea.swarm4j.spec.Spec;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 07/11/13
 *         Time: 18:25
 */
public interface UpstreamFactory {

    void onPeerConnected(EventRecipient peer);
    void onPeerDisconnected(EventRecipient peer);
    EventRecipient getUpstream(Spec spec);
}
