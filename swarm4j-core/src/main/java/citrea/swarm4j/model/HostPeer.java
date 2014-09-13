package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.Uplink;
import citrea.swarm4j.model.pipe.OpStream;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 08.09.2014
 *         Time: 23:33
 */
public interface HostPeer extends Uplink, ReferringToPeer {

    public Spec newEventSpec(SpecToken op);

    void accept(OpStream stream);

    void connect(URI upstreamURI, long reconnectTimeout) throws SwarmException;

    void connect(OpStream upstream) throws SwarmException;

    void disconnect() throws SwarmException;
}
