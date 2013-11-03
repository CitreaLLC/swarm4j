package citrea.swarm4j.model;

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

    protected Model(Swarm swarm, Spec id) {
        super(swarm, id, SpecQuant.MEMBER);
    }

    @Override
    protected Field createNewChild(Spec spec, JSONValue value) {
        return null;
    }

    public void addField(Field field) {
        addChild(field.getId(), field);
    }

    @Override
    public void on(Action action, Spec spec, JSONValue version, EventRecipient source) throws SwarmException {

        if (version != null && source != null) {
            //iterate all fields of this object
            for (SpecToken fieldId : getChildrenKeys()) {
                final Field field = getChild(fieldId);
                SpecToken fldVersion = field.getVersion();
                JSONValue maxKnownVersion = version.getFieldValue(fldVersion.getExt());
                //if we have newer version of the field
                if (fldVersion.getBare().compareTo(maxKnownVersion.getValueAsStr()) > 0) {
                    //send new version of the field
                    source.set(field.getSpec().overrideToken(SpecQuant.VERSION, fldVersion), field.getValue(), this);
                }
            }
        }

        //add source as this object listener
        addListener(source);

        if (Action.reOn == action) {
            return; // don't respond on .reOn
        }

        //send .reOn
        source.on(Action.reOn, getSpec(), this.getVersion(), this);
    }

    @Override
    public void off(Action action, Spec spec, EventRecipient source) throws SwarmException {
        //remove source from this object listeners
        removeListener(source);
    }

    @Override
    public void set(Spec spec, JSONValue diff, EventRecipient source) throws SwarmException {
        throw new SwarmNoChildException(spec);
    }

    public void init(Set<Type.FieldDescription> fieldDescriptions, JSONValue fieldValues) throws SwarmException {
        for (Type.FieldDescription descr: fieldDescriptions) {
            Spec fieldSpec = getSpec().overrideToken(SpecQuant.MEMBER, descr.getName());
            Field fld = new Field(swarm, fieldSpec, descr);
            addField(fld);
            JSONValue fieldValue = fieldValues.getFieldValue(descr.getNameAsStr());
            if (fieldValue == null) {
                fieldValue = descr.getDefaultValue();
            }
            fld.init(fieldValue);
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
}
