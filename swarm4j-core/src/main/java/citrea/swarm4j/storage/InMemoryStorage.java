package citrea.swarm4j.storage;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecQuant;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;
import com.sun.corba.se.impl.orbutil.concurrent.Sync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 /*
 * In a real storage implementation, state and log often go into
 * different backends, e.g. the state is saved to SQL/NoSQL db,
 * while the log may live in a key-value storage.
 * As long as the state has sufficient versioning info saved with
 * it (like a version vector), we may purge the log lazily, once
 * we are sure that the state is reliably saved. So, the log may
 * overlap with the state (some ops are already applied). That
 * provides some necessary resilience to workaround the lack of
 * transactions across backends.
 * In case third parties may write to the backend, go figure
 * some way to deal with it (e.g. make a retrofit operation).
 *
 * @author aleksisha
 *         Date: 26.08.2014
 *         Time: 00:33
 */
public class InMemoryStorage extends Storage {

    public static final Logger logger = LoggerFactory.getLogger(InMemoryStorage.class);

    // TODO async storage
    private Map<Spec, Map<Spec, JSONValue>> tails = new HashMap<Spec, Map<Spec, JSONValue>>();
    private Map<Spec, JSONValue> states = new HashMap<Spec, JSONValue>();

    protected Map<Spec, List<OpRecipient>> listeners;

    public InMemoryStorage(SpecToken id) {
        super(id);
        // many implementations do not push changes
        // so there are no listeners
        this.listeners = null;
    }

    @Override
    public void op(Spec spec, JSONValue val, OpRecipient source) throws SwarmException {
        if (!this.writeOp(spec, val, source)) {
            // The storage piggybacks on the object's state/log handling logic
            // First, it adds an op to the log tail unless the log is too long...
            // ...otherwise it sends back a subscription effectively requesting
            // the state, on state arrival zeroes the tail.
            source.deliver(spec.overrideToken(Syncable.REON), new JSONValue(Syncable.INIT.toString()), this);
        }
    }

    @Override
    protected void appendToLog(Spec ti, JSONValue verop2val) throws SwarmException {
        throw new UnsupportedOperationException("Not supported for InMemoryStorage");
    }

    @Override
    public void patch(Spec spec, JSONValue patch) throws SwarmException {
        this.writeState(spec, patch);
    }

    @Override
    public Spec getTypeId() {
        return new Spec(Host.HOST, this.getPeerId());
    }

    @Override
    public void on(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
        Spec ti = spec.getTypeId();

        if (this.listeners != null) {
            List<OpRecipient> ls = this.listeners.get(ti);
            if (ls == null) {
                ls = new ArrayList<OpRecipient>();
                this.listeners.put(ti, ls);
                ls.add(source);
            } else if (!ls.contains(source)) {
                ls.add(source);
            }
        }

        JSONValue state = this.readState(ti);
        if (state == null) {
            state = new JSONValue(new HashMap<String, JSONValue>());
            state.setFieldValue("_version", new JSONValue(SpecToken.ZERO_VERSION.toString()));
        }

        Map<Spec, JSONValue> tail = this.readOps(ti);

        if (tail != null) {
            JSONValue stateTail = state.getFieldValue("_tail");
            if (stateTail.isEmpty()) {
                stateTail = new JSONValue(new HashMap<String, JSONValue>());
            }
            for (Map.Entry<Spec, JSONValue> op : tail.entrySet()) {
                stateTail.setFieldValue(op.getKey().toString(), op.getValue());
            }
            state.setFieldValue("_tail", stateTail);
        }
        Spec tiv = ti.addToken(spec.getVersion());
        source.deliver(tiv.addToken(Syncable.PATCH), state, this);
        source.deliver(tiv.addToken(Syncable.REON), new JSONValue(Storage.stateVersionVector(state)), this);
    }

    @Override
    public void off(Spec spec, OpRecipient source) throws SwarmException {
        if (this.listeners == null) {
            return;
        }
        Spec ti = spec.getTypeId();
        List<OpRecipient> ls = this.listeners.get(ti);
        if (ls == null) {
            return;
        }
        if (ls.contains(source)) {
            if (ls.size() == 1) {
                this.listeners.remove(ti);
            } else {
                ls.remove(source);
            }
        }
    }

    protected void writeState(Spec spec, JSONValue state) {
        Spec ti = spec.getTypeId();
        this.states.put(ti, state);
        // tail is zeroed on state flush
        this.tails.put(ti, new HashMap<Spec, JSONValue>());
    }

    protected boolean writeOp(Spec spec, JSONValue value, OpRecipient source) {
        Spec ti = spec.getTypeId();
        Spec vo = spec.getVersionOp();
        Map<Spec, JSONValue> tail = this.tails.get(ti);
        if (tail == null) {
            tail = new HashMap<Spec, JSONValue>();
            this.tails.put(ti, tail);
        }
        if (tail.containsKey(vo)) {
            logger.warn("op replay @storage");
        }
        tail.put(vo, value);
        int count = tail.size();
        return (count < 3);
    }

    protected JSONValue readState(Spec ti) {
        return this.states.get(ti);
    }

    protected Map<Spec, JSONValue> readOps(Spec ti) {
        return this.tails.get(ti);
    }

}
