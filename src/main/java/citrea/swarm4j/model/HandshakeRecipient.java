package citrea.swarm4j.model;

import citrea.swarm4j.spec.SpecToken;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 01/11/13
 *         Time: 16:57
 */
public interface HandshakeRecipient extends EventRecipient {

    void setPeerId(SpecToken peerId);
    void setClientTs(String clientTs);
}
