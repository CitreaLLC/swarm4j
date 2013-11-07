package citrea.swarm4j.server;

import citrea.swarm4j.model.EventRecipient;
import citrea.swarm4j.spec.SpecToken;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 01/11/13
 *         Time: 16:57
 */
public interface HandshakeAware extends EventRecipient {

    SpecToken getPeerId();
    void setPeerId(SpecToken peerId);
    void setClientTs(String clientTs);
    boolean isHandshaken();
}
