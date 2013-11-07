package citrea.swarm4j.server;

import citrea.swarm4j.model.*;
import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecQuant;
import citrea.swarm4j.spec.SpecToken;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 29/10/13
 *         Time: 01:09
 */
public class Swarm extends AbstractEventRelay<AbstractEventRelay> implements EventRecipient {

    private SpecToken frozen = null;
    private int freezes = 0;
    private String lastTs = "";
    private int seq = 0;

    @Autowired
    private UpstreamFactory upstreamFactory;

    public Swarm(SpecToken typeId, SpecToken procId) {
        super(null, new Spec(typeId, procId), SpecQuant.TYPE);
        this.swarm = this;
        this.seq = 0;
        this.lastTs = procId.toString();
    }

    @Override
    protected void validate(Action action, Spec spec, JSONValue value, EventRecipient source) throws SwarmValidationException {
        logger.trace("validate action={} spec={} value={}", spec, value);
        if (source instanceof HandshakeAware) {
            HandshakeAware ha = (HandshakeAware) source;
            if (!ha.isHandshaken() && !getTypeId().equals(spec.getType())) {
                throw new SwarmValidationException(spec, "not handshaken");
            }
        }
    }

    @Override
    public void on(Action action, Spec spec, JSONValue value, EventRecipient source) throws SwarmException {
        logger.trace("on action={} spec={} value={}", action, spec, value);
        if (getTypeId().equals(spec.getType())) { //handshake ?

            if (source instanceof HandshakeAware) {
                HandshakeAware ha = (HandshakeAware) source;
                ha.setPeerId(spec.getId());
                ha.setClientTs(value.getValueAsStr());

                upstreamFactory.onPeerConnected(ha);
            }

            if (Action.reOn == action) {
                return; //don't respond on 'reOn' action
            }

            Spec reSpec = getSpec();
            source.on(Action.reOn, reSpec, new JSONValue(getLastTs()), this);
        } else {
            throw new SwarmNoChildException(spec, getChildKey());
        }
    }

    @Override
    public void off(Action action, Spec spec, EventRecipient source) throws SwarmException {
        logger.trace("off action={} spec={}", action, spec);
        if (getTypeId().equals(spec.getType())) {

            upstreamFactory.onPeerDisconnected(source);

        } else {
            throw new SwarmNoChildException(spec, getChildKey());
        }
    }

    @Override
    public void set(Spec spec, JSONValue value, EventRecipient listener) throws SwarmException {
        logger.trace("set unsupported");
        throw new SwarmNoChildException(spec, getChildKey());
    }

    @Override
    protected Type createNewChild(Spec spec, JSONValue value) {
        return null;
    }

    public void registerType(Type type) {
        addChild(type.getId(), type);
    }

    protected Date generateNewDate() {
        return new Date();
    }

    public SpecToken newVersion() throws SwarmException {
        if (this.getId() == null || this.getId().getExt() == null) {
            //TODO correct exception
            throw new SwarmException("Swarm.id not set");
        }
        if (this.frozen != null) {
            return this.frozen;
        }
        String ts = SpecToken.date2ts(generateNewDate());
        String seq;
        if (ts.equals(this.lastTs)) {
            seq = SpecToken.int2base(++this.seq, 2); // max ~4000Hz
        } else {
            this.seq = 0;
            seq = "";
        }
        this.lastTs = ts;
        SpecToken res = new SpecToken(ts + seq, this.getId().getExt());
        logger.trace("newVersion res={}", res);
        return res;
    }

    public void freeze() throws SwarmException {
        this.freezes++;
        if (this.freezes > 0)
            this.frozen = newVersion();
    }

    public void thaw() {
        this.freezes--;
        if (this.freezes == 0) {
            this.frozen = null;
        }
    }

    public String getLastTs() {
        return lastTs;
    }

    public SpecToken getTypeId() {
        return getSpec().getType();
    }

    public EventRecipient getUpstream(Spec spec) {
        return this.upstreamFactory.getUpstream(spec);
    }
}
