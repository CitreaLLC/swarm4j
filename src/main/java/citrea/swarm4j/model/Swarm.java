package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecQuant;
import citrea.swarm4j.spec.SpecToken;
import org.json.JSONException;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 29/10/13
 *         Time: 01:09
 */
public class Swarm extends AbstractEventRelay<AbstractEventRelay> implements SubscribeEventListener {

    private SpecToken frozen = null;
    private int freezes = 0;
    private String lastTs = "";
    private int seq = 0;

    public Swarm(SpecToken typeId, SpecToken procId) {
        super(null, new Spec(typeId, procId), SpecQuant.TYPE);
        this.swarm = this;
        this.seq = 0;
        this.lastTs = procId.toString();
    }

    @Override
    public void on(Spec spec, JSONValue value, SwarmEventListener source) throws SwarmException {

        if (getTypeId().equals(spec.getType())) { //handshake ?

            if (source instanceof HandshakeListener) {
                HandshakeListener hl = (HandshakeListener) source;
                hl.setPeerId(spec.getId());
                hl.setClientTs(value.getValueAsStr());
            }
            if (source instanceof SubscribeReplyListener) {
                try {
                    Spec reSpec = getSpec().overrideToken(SpecQuant.MEMBER, SpecToken.reOn);
                    ((SubscribeReplyListener) source).reOn(reSpec, new JSONValue(getLastTs()));
                } catch (JSONException e) {
                    throw new SwarmException("json generation error in Swarm.on");
                }
            }
        } else {
            throw new SwarmNoChildException(spec);
        }
    }

    @Override
    public void off(Spec spec, SwarmEventListener source) throws SwarmException {
        if (!getTypeId().equals(spec.getType())) {
            throw new SwarmNoChildException(spec);
        }
    }

    @Override
    public void set(Spec spec, JSONValue value, SwarmEventListener listener) throws SwarmException {
        throw new SwarmNoChildException(spec);
    }

    @Override
    protected Type createNewChild(Spec spec, JSONValue value) {
        return null;
    }

    public void registerType(Type type) {
        addChild(type.getId(), type);
    }

    public SpecToken newVersion() throws SwarmException {
        if (this.getId() == null || this.getId().getExt() == null) {
            //TODO correct exception
            throw new SwarmException("Swarm.id not set");
        }
        if (this.frozen != null) {
            return this.frozen;
        }
        String ts = SpecToken.date2ts(new Date());
        String seq;
        if (ts.equals(this.lastTs)) {
            seq = SpecToken.int2base(++this.seq, 2); // max ~4000Hz
        } else {
            this.seq = 0;
            seq = "";
        }
        this.lastTs = ts;
        return new SpecToken(ts + seq, this.getId().getExt());
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
}
