package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.*;
import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.meta.FieldMeta;
import citrea.swarm4j.model.meta.OperationMeta;
import citrea.swarm4j.model.meta.TypeMeta;
import citrea.swarm4j.model.oplog.LogDistillator;
import citrea.swarm4j.model.oplog.NoLogDistillator;
import citrea.swarm4j.model.spec.*;
import citrea.swarm4j.util.ChainedIterators;
import citrea.swarm4j.model.value.JSONValue;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 21.06.2014
 *         Time: 15:49
 */
public abstract class Syncable implements SomeSyncable, SubscriptionAware {

    public static final String RE_METHOD_NAME = "^[a-z][a-z0-9]*([A-Z][a-z0-9]*)*$";

    public static final SpecToken INIT = new SpecToken(".init");
    public static final SpecToken PATCH = new SpecToken(".patch");
    public static final SpecToken ERROR = new SpecToken(".error");

    protected Logger logger = LoggerFactory.getLogger(getClass());

    // TODO fix listeners description
    // listeners represented as objects that have deliver() method
    // ...so _lstn is like [server1, server2, storage, ',', view, listener]
    // The most correct way to specify a version is the version vector,
    // but that one may consume more space than the data itself in some cases.
    // Hence, version is not a fully specified version vector (see version()
    // instead). version is essentially is the greatest operation timestamp
    // (Lamport-like, i.e. "time+source"), sometimes amended with additional
    // timestamps. Its main features:
    // (1) changes once the object's state changes
    // (2) does it monotonically (in the alphanum order sense)
    protected List<Uplink> uplinks = new ArrayList<Uplink>();
    protected List<OpRecipient> listeners = new ArrayList<OpRecipient>();
    protected LogDistillator logDistillator = new NoLogDistillator();

    protected Host host;
    protected TypeMeta typeMeta;
    protected SpecToken type;
    protected SpecToken id;
    protected String version = null;
    protected String vector = null;
    protected Map<Spec, JSONValue> oplog = new HashMap<Spec, JSONValue>();

    public Syncable(SpecToken id, Host host) throws SwarmException {
        this.id = id;
        if (host == null) {
            if (!getClass().isAssignableFrom(Host.class)) {
                throw new IllegalArgumentException("host shouldn't be null");
            }
            // allowed for Host
            return;
        }
        this.setHost(host);
    }

    protected void setHost(Host host) throws SwarmException {
        this.host = host;
        this.typeMeta = this.getTypeMeta();
        this.type = this.typeMeta.getTypeToken();
        if (this.id == null) {
            this.id = new SpecToken(SpecQuant.ID, this.host.time());
            this.version = SpecToken.ZERO_VERSION.toString();
        }
        this.host.register(this);
        this.checkUplink();
    }

    /**
     * Applies a serialized operation (or a batch thereof) to this replica
     */
    @Override
    public synchronized void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
        logger.debug("{} <= ({}, {}, {})", this, spec, value, source);
        String opver = spec.getVersion().toString();
        value = value.clone();

        // sanity checks
        if (spec.getPattern() != SpecPattern.FULL) {
            source.deliver(spec.overrideToken(ERROR), new JSONValue("malformed spec"), OpRecipient.NOOP);
            return;
        }

        if (this.id == null) {
            source.deliver(spec.overrideToken(ERROR), new JSONValue("undead object invoked"), OpRecipient.NOOP);
            return;
        }

        String error = this.validate(spec, value);
        if (error != null && !"".equals(error)) {
            source.deliver(spec.overrideToken(ERROR), new JSONValue("invalid input, " + error), OpRecipient.NOOP);
            return;
        }

        if (!this.acl(spec, value, source)) {
            source.deliver(spec.overrideToken(ERROR), new JSONValue("access violation"), OpRecipient.NOOP);
            return;
        }

        //TODO if Swarm.debug
        //this.log(spec, value, this, source);

        try{
            SpecToken op = spec.getOp();
            OperationMeta opMeta = this.typeMeta.getOperationMeta(op);
            if (opMeta == null) {
                this.unimplemented(spec, value, source);
                return;
            }

            switch (opMeta.getKind()) {
                case Logged:
                    if (this.isReplay(spec)) return; // it happens
                    // invoke the implementation

                    opMeta.invoke(this, spec, value, source);

                    // once applied, may remember in the log...
                    if (PATCH.equals(op)) {
                        if (!this.oplog.isEmpty()) {
                            this.oplog.put(spec.getVersionOp(), value);
                        }
                        // this.version is practically a label that lets you know whether
                        // the state has changed. Also, it allows to detect some cases of
                        // concurrent change, as it is always set to the maximum version id
                        // received by this object. Still, only the full version vector may
                        // precisely and uniquely specify the current version (see version()).
                        this.version = (opver.compareTo(this.version) > 0) ? opver : this.version + opver;
                    }
                    // ...and relay further to downstream replicas and various listeners
                    this.emit(spec, value, source);
                    break;

                case Neutral:
                    // invoke the implementation
                    opMeta.invoke(this, spec, value, source);
                    // and relay to listeners
                    this.emit(spec, value, source);
                    break;

                case Remote:
                    // TODO ???
                default:
                    this.unimplemented(spec, value, source);
            }
        } catch (Exception ex) {
            // log and rethrow; don't relay further; don't log
            logger.error("deliver({}, {}, {}) exception: ", spec, value, source, ex);
            this.error(spec, new JSONValue("method execution failed: " + ex.toString()), source);
        }
    }

    private Spec typeId;
    /**
     * @return specifier "/Type#objid"
     */
    @Override
    public Spec getTypeId() {
        if (typeId == null) {
            if (this.id != null) {
                typeId = new Spec(this.type, this.id);
            } else {
                return new Spec(this.type, SpecToken.NO_ID);
            }
        }
        return typeId;
    }

    @Override
    public SpecToken getId() {
        return id;
    }

    /**
     * Generates new specifier with unique version
     * @param op operation
     * @return {Spec}
     */
    public Spec newEventSpec(SpecToken op) {
        return this.getTypeId()
                .addToken(new SpecToken(SpecQuant.VERSION, this.host.time()))
                .addToken(op);
    }

    /**
     * Returns current object state specifier
     * @return {string} specifier "/Type#objid!version+source[!version+source2...]"
     */
    public Spec getStateSpec() {
        return this.getTypeId().addToken(this.version); //?
    }

    /**
     * Notify all the listeners of a state change (i.e. the operation applied).
     */
    public void emit(Spec spec, JSONValue value, OpRecipient src) throws SwarmException {
        @SuppressWarnings("unchecked") Iterator<OpRecipient> it = new ChainedIterators<OpRecipient>(
                this.uplinks.iterator(),
                this.listeners.iterator()
        );
        SpecToken op = spec.getOp();
        OperationMeta opMeta = this.typeMeta.getOperationMeta(op);
        if (opMeta == null) {
            throw new SwarmException("No method found: " + op.getBody());
        }
        boolean is_neutrals = opMeta.getKind() == SwarmOperationKind.Neutral;
        if (it.hasNext()) {
            List<OpRecipient> notify = new ArrayList<OpRecipient>();
            while (it.hasNext()) {
                OpRecipient l = it.next();
                // skip empties, deferreds and the source
                if (l == null || l == src) continue;
                if (is_neutrals && !(l instanceof OpFilter)) continue;
                if (l instanceof OpFilter && !((OpFilter) l).getOp().equals(op)) continue;

                notify.add(l);
            }
            for (OpRecipient l : notify) { // screw it I want my 'this'
                try {
                    l.deliver(spec, value, this);
                } catch (Exception ex) {
                    //TODO log console.error(ex.message, ex.stack);
                }
            }
        }
        /*TODO reactions
        var r = this._reactions[spec.op()];
        if (r) {
            r.constructor!==Array && (r = [r]);
            for (i = 0; i < r.length; i++) {
                r[i] && r[i].call(this, spec, value, src);
            }
        } */
    }

    public void trigger(String event, JSONValue params) throws SwarmException {
        Spec spec = this.newEventSpec(new SpecToken("." + event));
        this.deliver(spec, params, OpRecipient.NOOP);
    }

    /**
     * Blindly applies a JSON changeset to this model.
     * @param values field values
     */
    public void apply(JSONValue values) throws SwarmException {
        for (String fieldName : values.getFieldNames()) {
            if (fieldName.startsWith("_")) {
                //special field: _version, _tail, _vector, _oplog
                continue;
            }
            FieldMeta meta = this.getTypeMeta().getFieldMeta(fieldName);
            if (meta == null) {
                logger.warn("{}.apply({}): Trying to modify unknown field: {}", this, values, fieldName);
                continue;
            }

            meta.set(this, values.getFieldValue(fieldName));
        }
    }

    /**
     * @return the version vector for this object
     * @see SpecMap
     */
    public SpecMap version() {
        // distillLog() may drop some operations; still, those need to be counted
        // in the version vector; so, their Lamport ids must be saved in this.vector
        SpecMap map = new SpecMap();
        if (this.version != null) {
            map.add(this.version);
        }
        if (this.vector != null) {
            map.add(this.vector);
        }
        if (!this.oplog.isEmpty()) {
            for (Spec op : this.oplog.keySet()) {
                map.add(op);
            }
        }
        return map; // TODO return the object, let the consumer trim it to taste
    }

    /**
     * Produce the entire state or probably the necessary difference
     * to synchronize a replica which is at version *base*.
     * @return {{version:String, _tail:Object, *}} a state object
     * that must survive JSON.parse(JSON.stringify(obj))
     *
     * The size of a Model's distilled log is capped by the number of
     * fields in an object. In practice, that is a small number, so
     * Model uses its distilled log to transfer state (no snapshots).
     */
    public JSONValue diff(SpecToken base) {
        //var vid = new Spec(this.version).get('!'); // first !token
        //var spec = vid + '.patch';
        this.distillLog(); // TODO optimize?
        Map<String, JSONValue> patch = new HashMap<String, JSONValue>();
        if (base != null && !SpecToken.ZERO_VERSION.equals(base)) {
            SpecMap map = new SpecMap(base.toString());
            Map<String, JSONValue> tail = new HashMap<String, JSONValue>();
            for (Map.Entry<Spec, JSONValue> op : this.oplog.entrySet()) {
                Spec spec = op.getKey();
                if (!map.covers(spec.getVersion())) {
                    tail.put(spec.toString(), op.getValue());
                }
            }
            patch.put("_tail", new JSONValue(tail));
        } else {
            Map<String, JSONValue> tail = new HashMap<String, JSONValue>();
            for (Map.Entry<Spec, JSONValue> op : this.oplog.entrySet()) {
                tail.put(op.getKey().toString(), op.getValue());
            }
            patch.put("_version", new JSONValue(SpecToken.ZERO_VERSION.toString()));
            patch.put("_tail", new JSONValue(tail));
        }
        return new JSONValue(patch);
    }

    protected Map<String, JSONValue> distillLog() {
        return this.logDistillator.distillLog(this.oplog);
    }

    /**
     * whether the update source (author) has all the rights necessary
     * @return {boolean}
     */
    public boolean acl(Spec spec, JSONValue val, OpRecipient src) {
        return true;
    }

    /**
     * Check operation format/validity (recommendation: don't check against the current state)
     * @return '' if OK, error message otherwise.
     */
    public String validate(Spec spec, JSONValue val) throws SwarmException {
        // TODO add causal stability violation check  Swarm.EPOCH  (+tests)
        return "";
    }

    /**
     * whether this op was already applied in the past
     * @return {boolean}
     */
    public boolean isReplay(Spec spec) {
        if (this.version == null) return false;

        SpecToken opver = spec.getVersion();
        if (opver.toString().compareTo(this.version) > 0) return false;

        Spec version_op = spec.getVersionOp();
        return this.oplog.containsKey(version_op) ||
                this.version().covers(opver);
    }

    /**
     * External objects (those you create by supplying an id) need first to query
     * the uplink for their state. Before the state arrives they are stateless.
     * @return {boolean}
     */
    public boolean hasState() {
        return this.version != null;
    }

    public void reset() {
        //TODO implement reset()
    }

    /**
     * Subscribe to the object's operations;
     * the upstream part of the two-way subscription
     *  on() with a full filter:
     *  @param spec /Mouse#Mickey!now.on
     *  @param filterValue !since.event
     *  @param source callback
     */
    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void on(Spec spec, JSONValue filterValue, OpRecipient source) throws SwarmException {   // WELL  on() is not an op, right?
        // if no listener is supplied then the object is only
        // guaranteed to exist till the next Host.gc() run
        if (source == null) return;

        // stateless objects fire no events; essentially, on() is deferred
        if (this.version == null && !this.isUplinked()) {
            this.addListener(
                    new OpFilter(
                            new Deferred(spec, filterValue, source),
                            REON
                    )
            );
            return; // defer this call till uplinks are ready
        }

        if (JSONValue.NULL != filterValue) {
            Spec filter = new Spec(filterValue.getValueAsStr());
            SpecToken baseVersion = filter.getVersion();
            SpecToken filter_by_op = filter.getOp();

            if (filter_by_op != null) {
                if (INIT.equals(filter_by_op)) {
                    JSONValue diff_if_needed = baseVersion != null ? this.diff(baseVersion) : JSONValue.NULL;
                    source.deliver(spec.overrideToken(PATCH), diff_if_needed, this);
                    // use once()
                    return;
                }

                source = new OpFilter(source, filter_by_op);
            }

            if (baseVersion != null) {
                JSONValue diff = this.diff(baseVersion);
                if (!diff.isEmpty()) {
                    source.deliver(spec.overrideToken(PATCH), diff, this); // 2downlink
                }
                source.deliver(spec.overrideToken(REON), new JSONValue(this.version().toString()), this);
            }
        }

        this.addListener(source);
        // TODO repeated subscriptions: send a diff, otherwise ignore
    }

    // should be generated?
    @Override
    public void on(JSONValue evfilter, OpRecipient source) throws SwarmException {
        this.on(newEventSpec(ON), evfilter, source);
    }

    /**
     * downstream reciprocal subscription
     */
    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void reon(Spec spec, JSONValue base, OpRecipient source) throws SwarmException {
        if (base.isEmpty()) return;

        JSONValue diff = this.diff(new SpecToken(base.getValueAsStr()));
        if (diff.isEmpty()) return;

        source.deliver(spec.overrideToken(PATCH), diff, this); // 2uplink
    }

    /** Unsubscribe */
    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void off(Spec spec, OpRecipient repl) throws SwarmException {
        this.removeListener(repl);
    }

    // should be generated?
    @Override
    public void off(OpRecipient source) throws SwarmException {
        this.deliver(this.newEventSpec(OFF), JSONValue.NULL, source);
    }

    /** Reciprocal unsubscription */
    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void reoff(Spec spec, OpRecipient source) throws SwarmException {
        this.removeListener(source);
        if (this.id != null) this.checkUplink();
    }

    /**
     * As all the event/operation processing is asynchronous, we
     * cannot simply throw/catch exceptions over the network.
     * This method allows to send errors back asynchronously.
     * Sort of an asynchronous complaint mailbox :)
     */
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void error(Spec spec, JSONValue value, OpRecipient source) {
        this.log(spec.overrideToken(ERROR), value, this, source);
    }

    /**
     * A state of a Syncable CRDT object is transferred to a replica using
     * some combination of POJO state and oplog. For example, a simple LWW
     * object (Last Writer Wins, see Model below) uses its distilled oplog
     * as the most concise form. A CT document (Causal Trees) has a highly
     * compressed state, its log being hundred times heavier. Hence, it
     * mainly uses its plain state, but sometimes its log tail as well. The
     * format of the state object is POJO plus (optionally) special fields:
     * oplog, _tail, _vector, version (the latter flags POJO presence).
     * In either case, .state is only produced by diff() (+ by storage).
     * Any real-time changes are transferred as individual events.
     * @param spec specifier
     * @param state patch json
     * @param source source of operation
     */
    @SwarmOperation(kind = SwarmOperationKind.Logged)
    public void patch(Spec spec, JSONValue state, OpRecipient source) throws SwarmException {
        Map<String, JSONValue> tail = new HashMap<String, JSONValue>();
        Spec typeid = spec.getTypeId();
        List<Uplink> uplinksBak = this.uplinks;
        List<OpRecipient> listenersBak = this.listeners;
        // prevent events from being fired
        this.uplinks = new ArrayList<Uplink>(0);
        this.listeners = new ArrayList<OpRecipient>(0);

            /*if (state._version === '!0') { // uplink knows nothing FIXME dubious
                if (!this._version) this._version = '!0';
            }*/

        JSONValue state_version = state.getFieldValue("_version");
        if (!state_version.isEmpty() /* && state._version !== '!0'*/) {
            // local changes may need to be merged into the received state
            if (!this.oplog.isEmpty()) {
                for (Map.Entry<Spec, JSONValue> op : this.oplog.entrySet()) {
                    tail.put(op.getKey().toString(), op.getValue());
                }
                this.oplog.clear();
            }
            if (this.vector != null) {
                this.vector = null;
            }
            // TODO zero everything
            /*
            for (FieldMeta field : this.typeMeta.getAllFields()) {
                field.set(this, JSONValue.NULL);
            }
            */
            // set default values
            this.reset();

            this.apply(state);
            this.version = state_version.getValueAsStr();
            JSONValue state_oplog = state.getFieldValue("_oplog");
            if (!state_oplog.isEmpty()) {
                for (String op_spec : state_oplog.getFieldNames()) {
                    this.oplog.put(new Spec(op_spec), state_oplog.getFieldValue(op_spec));
                }
            }
            JSONValue state_vector = state.getFieldValue("_vector");
            if (!state_vector.isEmpty()) {
                this.vector = state_vector.getValueAsStr();
            }
        }
        // add the received tail to the local one
        tail.putAll(state.getFieldValue("_tail").getValueAsMap());
        // appply the combined tail to the new state

        String[] specs = tail.keySet().toArray(new String[tail.size()]);
        Arrays.sort(specs);
        // there will be some replays, but those will be ignored
        for (String a_spec : specs) {
            this.deliver(typeid.addToken(a_spec), tail.get(a_spec), source);
        }

        this.uplinks = uplinksBak;
        this.listeners = listenersBak;
    }

    /**
     * Uplink connections may be closed or reestablished so we need
     * to adjust every object's subscriptions time to time.
     */
    protected void checkUplink() throws SwarmException {
        List<Uplink> subscribeTo = this.host.getSources(this.getTypeId());
        // the plan is to eliminate extra subscriptions and to
        // establish missing ones; that only affects outbound subs
        List<Uplink> unsubscribeFrom = new ArrayList<Uplink>();
        for (Uplink up : this.uplinks) {
            if (up == null) {
                continue;
            }
            if (up instanceof PendingUplink) {
                up = ((PendingUplink) up).getInner();
            }
            int up_idx = subscribeTo.indexOf(up);
            if (up_idx > -1) {
                // is already subscribed or awaiting for response
                subscribeTo.remove(up_idx);
            } else {
                // don't need this uplink anymore
                unsubscribeFrom.add(up);
            }
        }
        // unsubscribe from old
        for (Uplink up : unsubscribeFrom) {
            up.deliver(this.newEventSpec(OFF), JSONValue.NULL, this);
        }
        // subscribe to the new
        for (Uplink new_uplink : subscribeTo) {
            if (new_uplink == null) {
                continue;
            }

            Spec onSpec = this.newEventSpec(ON);
            this.addUplink(new PendingUplink(this, new_uplink, onSpec.getVersion()));
            new_uplink.deliver(onSpec, new JSONValue(this.version().toString()), this);
        }
    }

    /**
     * returns a Plain Javascript Object with the state
     */
    public JSONValue getPOJO(boolean addVersionInfo) throws SwarmException {
        Map<String, JSONValue> pojo = new HashMap<String, JSONValue>();
        //TODO defaults
        for (FieldMeta field : this.typeMeta.getAllFields()) {
            JSONValue fieldValue = field.get(this);
            pojo.put(field.getName(), fieldValue);
        }
        if (addVersionInfo) {
            pojo.put("_id", new JSONValue(this.id.toString())); // not necassary
            pojo.put("_version", new JSONValue(this.version));
            if (this.vector != null) {
                pojo.put("_vector", new JSONValue(this.vector));
            }
            if (!this.oplog.isEmpty()) {
                Map<String, JSONValue> oplog = new HashMap<String, JSONValue>(this.oplog.size());
                for (Map.Entry<Spec, JSONValue> op : this.oplog.entrySet()) {
                    oplog.put(op.getKey().toString(), op.getValue());
                }
                pojo.put("_oplog", new JSONValue(oplog)); //TODO copy
            }
        }
        return new JSONValue(pojo);
    }

    /**
     * Sometimes we get an operation we don't support; not normally
     * happens for a regular replica, but still needs to be caught
     * @param spec operation specifier
     * @param value operation params
     * @param source operation source
     */
    public void unimplemented(Spec spec, JSONValue value, OpRecipient source) {
        logger.warn("{}.unimplemented({}, {}, {})", this, spec, value, source);
    }

    /**
     * Deallocate everything, free all resources.
     */
    public void close() throws SwarmException {
        // unsubscribe from uplinks
        Iterator<Uplink> itUplinks = uplinks.iterator();
        Spec spec = this.getTypeId();
        while (itUplinks.hasNext()) {
            OpRecipient uplink = itUplinks.next();
            if (uplink instanceof Peer) {
                uplink.deliver(this.newEventSpec(OFF), JSONValue.NULL, this);
            }
            itUplinks.remove();
        }
        // notify listeners of object closing
        Iterator<OpRecipient> itListeners = listeners.iterator();
        while (itListeners.hasNext()) {
            // FIXME no version token in spec ???
            itListeners.next().deliver(spec.addToken(REOFF), JSONValue.NULL, this);
            itListeners.remove();
        }

        this.host.unregister(this);
    }

    /**
     * Once an object is not listened by anyone it is perfectly safe
     * to garbage collect it.
     */
    public void gc() throws SwarmException {
        if (uplinks.size() == 0 && listeners.size() == 0) {
            this.close();
        }
    }

    public void log(Spec spec, JSONValue value, Syncable object, OpRecipient source) {
        if (ERROR.equals(spec.getOp())) {
            logger.warn("log: {}->{} {} {}", spec, value, object, source);
        } else {
            logger.debug("log: {}->{} {} {}", spec, value, object, source);
        }
    }

    public void once(JSONValue evfilter, OpRecipient fn) throws SwarmException, JSONException {
        this.on(evfilter, new OnceOpRecipient(this, fn));
    }

    public void addUplink(Uplink uplink) {
        logger.debug("{}.addUplink({})", this, uplink);
        this.uplinks.add(uplink);
    }

    public boolean isUplinked() {
        if (this.uplinks.isEmpty()) return false;

        for (Uplink peer : this.uplinks) {
            if (peer instanceof PendingUplink) {
                return false;
            }
        }
        return true;
    }

    public boolean hasUplink(Peer peer) {
        return this.uplinks.indexOf(peer) > -1;
    }

    protected void addListener(OpRecipient listener) {
        logger.debug("{}.addListener({})", this, listener);
        this.listeners.add(listener);
    }

    public void removeListener(OpRecipient listener) {
        logger.debug("{}.removeListener({})", this, listener);

        @SuppressWarnings("unchecked") Iterator<OpRecipient> it = new ChainedIterators<OpRecipient>(
                this.uplinks.iterator(),
                this.listeners.iterator()
        );

        while (it.hasNext()) {
            OpRecipient l = it.next();
            if (l == listener) {
                it.remove();
                return;
            }

            // @see FilteringOpRecipient#equals() implementation
            if (l.equals(listener)) {
                logger.debug("{}.removeListener(): actualRemoved={}", this, l);
                it.remove();
                return;
            }
        }
    }

    public TypeMeta getTypeMeta() throws SwarmException {
        return this.host.getTypeMeta(this.getClass());
    }

    public SpecToken getType() {
        return this.type;
    }

    @Override
    public SpecToken getPeerId() {
        return this.host == null ? null : this.host.getId();
    }

    @Override
    public String toString() {
        return getTypeId().toString();
    }

    protected class Deferred extends FilteringOpRecipient<OpRecipient> {

        private Spec spec;
        private JSONValue filter;

        public Deferred(Spec spec, JSONValue filter, OpRecipient source) {
            super(source);
            this.spec = spec;
            this.filter = filter;
        }

        @Override
        public boolean filter(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
            return !(Syncable.this.version == null && !Syncable.this.isUplinked());
        }

        @Override
        public void deliverInternal(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
            Syncable.this.removeListener(this);
            Syncable.this.deliver(this.spec, this.filter, this.getInner());
        }

        public OpRecipient getSource() {
            return getInner();
        }

        @Override
        public String toString() {
            return "" + Syncable.this.getTypeId() + ".Deferred{" +
                    "spec=" + spec +
                    ", filter=" + filter +
                    ", inner=" + inner +
                    '}';
        }
    }

}
