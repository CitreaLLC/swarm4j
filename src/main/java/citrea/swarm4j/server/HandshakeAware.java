package citrea.swarm4j.server;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 01/11/13
 *         Time: 16:57
 */
public interface HandshakeAware extends OpRecipient {

    Spec getPeerId();
    void setPeerId(SpecToken peerId);
    void setClientTs(String clientTs);
    boolean isHandshaken();
}
