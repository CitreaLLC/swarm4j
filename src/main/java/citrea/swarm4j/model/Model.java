package citrea.swarm4j.model;

import citrea.swarm4j.server.Swarm;
import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecQuant;
import citrea.swarm4j.spec.SpecToken;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 29/10/13
 *         Time: 01:01
 */
public class Model extends AbstractEventRelay<Field> implements EventRecipient {

    private boolean ready;
    private Map<EventRecipient, ActionSpecValueTriplet> pendingOns = new HashMap<EventRecipient, ActionSpecValueTriplet>();
    private EventRecipient upstream;

    protected Model(Swarm swarm, Spec id) {
        super(swarm, id, SpecQuant.MEMBER);
        this.ready = false;
        this.upstream = null;
    }

    public void addField(Field field) {
        addChild(field.getId(), field);
    }

    public EventRecipient getUpstream() {
        return upstream;
    }

    public void setUpstream(EventRecipient upstream) throws SwarmException {
        if (this.upstream == upstream) {
            return;
        } else if (this.upstream != null) {
            this.upstream.off(Action.off, getSpec(), this);
        }
        this.upstream = upstream;
        if (this.upstream != null) {
            this.upstream.on(Action.on, getSpec(), getVersion(), this);
        }
    }

    @Override
    protected Field createNewChild(Spec spec, JSONValue value) {
        return null;
    }

    @Override
    protected void validate(Action action, Spec spec, JSONValue value, EventRecipient source) throws SwarmValidationException {
        logger.trace("validate action={} spec={} value={}", action, spec, value);
        if (action == Action.set) {
            if (!isListenersContains(source) && source != this.upstream) {
                throw new SwarmValidationException(spec, "no subscription but *set received");
            }
        }
    }

    @Override
    public void on(Action action, Spec spec, JSONValue version, EventRecipient source) throws SwarmException {
        logger.trace("on action={} spec={} value={}", action, spec, version);

        //only after loaded from store or synced from other server
        if (!this.ready) {
            //add operation as pending
            pendingOns.put(source, new ActionSpecValueTriplet(action, spec, version));
            //set upstream
            setUpstream(swarm.getUpstream(spec));
            return;
        }

        if (Action.reOn == action && source == getUpstream()) { // *reOn received from upstream

            //process all pending *on operations received from downstream peers
            for (Map.Entry<EventRecipient, ActionSpecValueTriplet> e: pendingOns.entrySet()) {
                final EventRecipient peer = e.getKey();
                final ActionSpecValueTriplet triplet = e.getValue();

                this.on(triplet.getAction(), triplet.getSpec(), triplet.getValue(), peer);
            }
            pendingOns.clear();

        } else {
            //add source as this object listener
            addListener(source);

            if (version != null && source != null) {
                JSONValue diff = getDiff(version);
                if (!diff.isEmpty()) {
                    //send diff
                    source.set(getSpec(), diff, this);
                }
            }

            if (Action.reOn != action) {
                source.on(Action.reOn, getSpec(), this.getVersion(), this);
            }
        }
    }

    @Override
    public void off(Action action, Spec spec, EventRecipient source) throws SwarmException {
        logger.trace("off action={} spec={}", action, spec);
        //remove source from this object listeners
        removeListener(source);

        if (source == upstream) {
            this.upstream = null;
        }

        if (action != Action.reOff) {
            source.off(Action.reOff, spec, this);
        }

        //TODO check if the object can be garbage collected
    }

    @Override
    public void set(Spec spec, JSONValue diff, EventRecipient source) throws SwarmException {
        logger.trace("set spec={} value={}", spec, diff);

        //apply diff
        Spec objSpec = getSpec();
        if (diff != null) {
            for (String fieldSpecPostfix : diff.getFieldNames()) {
                final JSONValue value = diff.getFieldValue(fieldSpecPostfix);
                final Spec fieldSpec = new Spec(objSpec.toString() + fieldSpecPostfix);
                Field field = getChild(fieldSpec);
                field.set(fieldSpec, value, source);
            }
        }

        if (!this.ready) {
            this.ready = true;
        }
    }

    public void init(SpecToken id, Set<Type.FieldDescription> fieldDescriptions, JSONValue fieldValues) throws SwarmException {
        logger.trace("init id={}", id);
        for (Type.FieldDescription descr: fieldDescriptions) {
            Spec fieldSpec = getSpec().overrideToken(SpecQuant.MEMBER, descr.getName());
            Field fld = new Field(swarm, fieldSpec, descr);
            addField(fld);
            JSONValue fieldValue = null;
            if (fieldValues != null) {
                fieldValue = fieldValues.getFieldValue(descr.getNameAsStr());
            }
            if (fieldValue == null) {
                fieldValue = descr.getDefaultValue();
            }
            fld.init(id, fieldValue);
        }
    }

    /**
     * @return current version vector: {"author~ssn": "lastKnownVersion",...}
     * @throws SwarmException
     */
    public JSONValue getVersion() throws SwarmException {
        Map<String, String> versionVector = new HashMap<String, String>();
        for (SpecToken fieldId : getChildrenKeys()) {
            final Field field = getChild(fieldId);
            final SpecToken version = field.getVersion();
            final String bare = version.getBare();
            final String ext = version.getExt();
            String currentMaxVersion = versionVector.get(ext);
            if (currentMaxVersion == null || currentMaxVersion.compareTo(bare) <= 0) {
                versionVector.put(ext, bare);
            }
        }
        try {
            return new JSONValue(versionVector);
        } catch (JSONException e) {
            throw new SwarmException("error building json for version: " + e.getMessage(), e);
        }
    }

    public JSONValue getDiff(JSONValue version) throws SwarmException {
        logger.trace("getDiff base={}", version);
        Map<String, JSONValue> res = new HashMap<String, JSONValue>();
        //iterate all fields of this object
        for (SpecToken fieldId : getChildrenKeys()) {
            final Field field = getChild(fieldId);
            SpecToken fldVersion = field.getVersion();
            JSONValue maxKnownVersion = version.getFieldValue(fldVersion.getExt());
            //if we have newer version of the field
            if (maxKnownVersion != null &&
                    fldVersion.getBare().compareTo(maxKnownVersion.getValueAsStr()) > 0) {
                //send new version of the field
                res.put(field.getId().withQuant(SpecQuant.MEMBER) + fldVersion.withQuant(SpecQuant.VERSION), field.getValue());
            }
        }
        try {
            JSONValue diff = new JSONValue(res);
            logger.trace("getDiff base={} result={}", version, diff);
            return diff;
        } catch (JSONException e) {
            throw new SwarmException("error building json for version: " + e.getMessage(), e);
        }
    }
}
