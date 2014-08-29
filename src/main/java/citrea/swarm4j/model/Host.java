package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.callback.Peer;
import citrea.swarm4j.model.callback.Uplink;
import citrea.swarm4j.model.annotation.SwarmOperation;
import citrea.swarm4j.model.annotation.SwarmOperationKind;
import citrea.swarm4j.model.meta.TypeMeta;
import citrea.swarm4j.model.reflection.ReflectionTypeMeta;
import citrea.swarm4j.storage.Storage;
import citrea.swarm4j.server.WSWrapper;
import citrea.swarm4j.model.spec.*;
import citrea.swarm4j.model.value.JSONValue;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 21.06.2014
 *         Time: 15:51
 */
public class Host extends Syncable {
    public static final SpecToken HOST = new SpecToken("/Host");
    private Map<SpecToken, TypeMeta> knownTypes = new HashMap<SpecToken, TypeMeta>();

    Map<Spec, Syncable> objects = new HashMap<Spec, Syncable>();
    private String lastTs = "";
    private int tsSeq = 0;
    private int clockOffset = 0;
    private Map<Spec, Peer> sources = new HashMap<Spec, Peer>();
    private Storage storage = null;

    public Host(SpecToken id, Storage storage) throws SwarmException {
        super(id, null);
        this.setHost(this);
        if (storage != null) {
            this.storage = storage;
            this.storage.setHost(this);
            this.sources.put(this.getTypeId(), storage);
        }
    }

    protected void setHost(Host host) throws SwarmException {
        this.host = host;
        this.ensureTypeIdSet();
    }

    @Override
    public void deliver(Spec spec, JSONValue val, OpRecipient source) throws SwarmException {
        if (spec.getPattern() != SpecPattern.FULL) throw new SwarmException("incomplete operation spec");

        if (!HOST.equals(spec.getType())) {
            Spec typeid = spec.getTypeId();
            Syncable obj = this.get(typeid);
            if (obj != null) {
                obj.deliver(spec, val, source);
            }
        } else {
            super.deliver(spec, val, source);
        }
    }

    public <T extends Syncable> T get(Class<T> type) throws SwarmException {
        TypeMeta typeMeta = getTypeMeta(type);
        return newInstance(typeMeta, null);
    }

    public <T extends Syncable> T get(Spec spec) throws SwarmException {
        Spec typeid = spec.getTypeId();
        T res;
        if (typeid.getPattern() == SpecPattern.TYPE_ID) {
            res = (T) this.objects.get(typeid);
            if (res != null) {
                return res;
            }
        }                //TODO create new instance

        SpecToken type = typeid.getType();
        if (type == null) {
            throw new SwarmException("invalid spec (expecting \"/Type#id\" or \"/Type\")");
        }

        TypeMeta typeMeta = getTypeMeta(type);
        return newInstance(typeMeta, spec.getId());
    }

    private <T extends Syncable> T newInstance(TypeMeta typeMeta, SpecToken id) throws SwarmException {
        T res = (T) typeMeta.newInstance(id, this);
        //TODO defaults obj.apply(JSONValue.NULL);
        return res;
    }

    public void addSource(Spec spec, Peer peer) throws SwarmException {
        //TODO their time is off so tell them so  //FIXME ???
        Peer old = this.sources.get(peer.getTypeId());
        if (old != null) {
            old.deliver(this.newEventSpec(OFF), JSONValue.NULL, this);
        }

        this.sources.put(peer.getTypeId(), peer);
        if (ON.equals(spec.getOp())) {
            peer.deliver(this.newEventSpec(REON), JSONValue.NULL, this); // TODO offset
        }

        for (Syncable obj: this.objects.values()) {
            obj.checkUplink();
        }

        this.emit(spec, JSONValue.NULL, peer); // PEX hook
    }

    /**
     * Host forwards on() calls to local objects to support some
     * shortcut notations, like
     *          host.on('/Mouse',callback)
     *          host.on('/Mouse.init',callback)
     *          host.on('/Mouse#Mickey',callback)
     *          host.on('/Mouse#Mickey.init',callback)
     *          host.on('/Mouse#Mickey!baseVersion',repl)
     *          host.on('/Mouse#Mickey!base.x',trackfn)
     * The target object may not exist beforehand.
     * Note that the specifier is actually the second 3sig parameter
     * (value). The 1st (spec) reflects this /Host.on invocation only.
     */
    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void on(Spec spec, JSONValue evfilter, OpRecipient source) throws SwarmException {
        if (evfilter.isEmpty()) {// the subscriber needs "all the events"
            if (!(source instanceof Peer)) {
                throw new IllegalArgumentException("evfilter is empty but source is not a peer");
            }
            this.addSource(spec, (Peer) source);
            return;
        }

        Spec objon;
        if (evfilter.getValueAsStr().matches(Syncable.RE_METHOD_NAME)) { //this Host operation listening
            objon = this.getTypeId().getTypeId(); // "/Host#id"
        } else {
            objon = new Spec(evfilter.getValueAsStr()).getTypeId();
            if (objon.getType() == null) throw new IllegalArgumentException("no type mentioned");
        }

        if (HOST.equals(objon.getType())) {
            super.on(spec, evfilter, source);
        } else {
            if (objon.getId() == null) {
                objon = objon.addToken(spec.getVersion().overrideQuant(SpecQuant.ID));
            }
            objon = objon.addToken(spec.getVersion()).addToken(ON);
            this.deliver(objon, evfilter, source);

            // We don't do this as the object may have no state now.
            // return o;
            // Instead, use host.on('/Type#id.init', function(,,o) {})
        }
    }

    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void reon(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
        if (!HOST.equals(spec.getType())) throw new IllegalArgumentException("/NotHost");
        /// well.... TODO
        if (!(source instanceof Peer)) throw new IllegalArgumentException("src is not a Peer");
        this.addSource(spec, (Peer) source);
    }

    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void off(Spec spec, OpRecipient src) throws SwarmException {
        if (!(src instanceof Peer)) throw new IllegalArgumentException("src is not a Peer");

        Spec reoffSpec = new Spec(
                HOST,
                ((Peer) src).getTypeId().getId(),
                new SpecToken(SpecQuant.VERSION, this.time()),
                REOFF
        );
        src.deliver(reoffSpec, JSONValue.NULL, this);
        this.removeSource(spec, (Peer) src);
    }

    @Override
    @SwarmOperation(kind = SwarmOperationKind.Neutral)
    public void reoff(Spec spec, OpRecipient src) throws SwarmException {
        if (!(src instanceof Peer)) throw new IllegalArgumentException("src is not a Peer");
        this.removeSource(spec, (Peer) src);
    }

    private void removeSource(Spec spec, Peer peer) throws SwarmException {
        if (!HOST.equals(spec.getType())) throw new IllegalArgumentException("/NotHost");

        if (this.sources.get(peer.getTypeId()) != peer) {
            //TODO log console.error('peer unknown', peer._id); //throw new Error
            return;
        }
        this.sources.remove(peer.getTypeId());
        for (Map.Entry<Spec, Syncable> sp : this.objects.entrySet()) {
            Syncable obj = sp.getValue();
            if (obj.hasUplink(peer)) {
                obj.off(sp.getKey(), peer);
                obj.checkUplink();
            }
        }
    }

    /**
     * Returns an unique Lamport timestamp on every invocation.
     * Swarm employs 30bit integer Unix-like timestamps starting epoch at
     * 1 Jan 2010. Timestamps are encoded as 5-char base64 tokens; in case
     * several events are generated by the same process at the same second
     * then sequence number is added so a timestamp may be more than 5
     * chars. The id of the Host (+user~session) is appended to the ts.
     * @return unique timestamp
     */
    public String time() {
        long d = new Date().getTime() - SpecToken.EPOCH + this.clockOffset;
        String ts = SpecToken.int2base((int)(d / 1000), 5);
        String res = ts;
        if (ts.equals(this.lastTs)) {
            res += SpecToken.int2base(++this.tsSeq, 2); // max ~4000Hz
        } else {
            this.tsSeq = 0;
        }
        res += '+' + this.getId().getBody();
        this.lastTs = ts;
        this.version = "!" + res;
        return res;
    }

    /**
     * Returns an array of sources (caches,storages,uplinks,peers)
     * a given replica should be subscribed to. This default
     * implementation uses a simple consistent hashing scheme.
     * Note that a client may be connected to many servers
     * (peers), so the uplink selection logic is shared.
     * @param spec some object specifier
     * @return list of currently available uplinks for specified object
     */
    public List<Uplink> getSources(Spec spec) {
        //TODO spec must be /Type#id
        List<Uplink> uplinks = new ArrayList<Uplink>();
        int mindist = Integer.MAX_VALUE;
        Pattern rePeer = Pattern.compile("^swarm~"); // peers, not clients
        String target = spec.getId().getBody();
        Uplink closestPeer = null;

        Matcher m = rePeer.matcher(this.getId().getBody());
        if (m.matches()) {
            mindist = Host.hashDistance(this.getId().getBody(), target);
            closestPeer = this.storage;
        } else if (this.storage != null) {
            uplinks.add(this.storage); // client-side cache
        }

        for (Map.Entry<Spec, Peer> entry : this.sources.entrySet()) {
            String id = entry.getKey().getId().getBody();
            m = rePeer.matcher(id);
            if (!m.matches()) continue;

            int dist = Host.hashDistance(id, target);
            if (dist < mindist) {
                closestPeer = entry.getValue();
                mindist = dist;
            }
        }
        if (closestPeer != null) uplinks.add(closestPeer);
        return uplinks;
    }

    public Syncable register(Syncable obj) {
        Spec spec = obj.getTypeId();
        Syncable res = this.objects.get(spec);
        if (res == null) {
            this.objects.put(spec, obj);
            res = obj;
        }
        return res;
    }

    public void unregister(Syncable obj) {
        Spec spec = obj.getTypeId();
        // TODO unsubscribe from the uplink - swarm-scale gc
        if (this.objects.containsKey(spec)) {
            this.objects.remove(spec);
        }
    }

    public void disconnect() throws SwarmException {
        for (Map.Entry<Spec, Peer> entry : sources.entrySet()) {
            OpRecipient peer = entry.getValue();
            if (peer instanceof WSWrapper) {
                ((WSWrapper) peer).close();
            }
        }
    }

    // TODO Host event relay + PEX

    public void registerType(Class<? extends Syncable> type) throws SwarmException {
        this.knownTypes.put(new SpecToken(SpecQuant.TYPE, type.getSimpleName()), new ReflectionTypeMeta(type));
    }

    public TypeMeta getTypeMeta(SpecToken typeToken) throws SwarmException {
        TypeMeta res = knownTypes.get(typeToken);
        if (res == null) {
            throw new SwarmException("Unknown type: " + typeToken.toString());
        }
        return res;
    }

    public TypeMeta getTypeMeta(Class<? extends Syncable> type) throws SwarmException {
        SpecToken typeToken = new SpecToken(SpecQuant.TYPE, type.getSimpleName());
        TypeMeta res = knownTypes.get(typeToken);
        if (res != null) {
            return res;
        }

        try {
            this.registerType(type);
        } catch (SwarmException e) {
            throw new IllegalArgumentException("Error registering type: " + type.getName(), e);
        }
        return getTypeMeta(typeToken);
    }

    public static int hashDistance(Object ipeer, Object obj) {
        int obj_hash;
        String peer;
        if (obj instanceof Number) {
            obj_hash = ((Number) obj).intValue();
        } else if (obj instanceof Syncable) {
            obj_hash = MurMurHash3.x86_32(((Syncable) obj).getTypeId().getId().getBody());
        } else {
            obj_hash = MurMurHash3.x86_32(obj.toString());
        }

        if (ipeer instanceof Syncable) {
            peer = ((Syncable) ipeer).getId().getBody();
        } else if (ipeer instanceof Spec) {
            peer = ((Spec) ipeer).getId().getBody();
        } else {
            peer = ipeer.toString();
        }

        int dist = Integer.MAX_VALUE;
        for (int i = 0; i < 3; i++) { //TODO 3 ~ HASH_POINTS
            int hash = MurMurHash3.x86_32(peer + ":" + i);
            dist = Math.min(dist, hash ^ obj_hash);
        }
        return dist;
    }
}