package citrea.swarm4j.model;

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
public class Model extends AbstractEventRelay<Field> implements SubscribeEventListener {

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
    public void on(Spec spec, JSONValue version, SwarmEventListener source) throws SwarmException {

        if (version != null && source != null) {
            final JSONValue diff = getDiff(version);
            if (!diff.isEmpty()) {
                source.set(spec, diff, this);
            }
        }

        if (SpecToken.reOn.equals(spec.getMember())) {
            return; // don't respond on .reOn
        }

        //add source as this object listener
        addListener(source);

        //send .reOn
        source.set(spec.overrideToken(SpecQuant.MEMBER, SpecToken.reOn), this.getVersion(), this);

        if (source instanceof SubscribeReplyListener) {
            ((SubscribeReplyListener) source).reOn(spec, version);
        }
    }

    @Override
    public void off(Spec spec, SwarmEventListener source) throws SwarmException {
    }

    @Override
    public void set(Spec spec, JSONValue diff, SwarmEventListener source) throws SwarmException {
        this.applyDiff(diff, source);
    }

    private void applyDiff(JSONValue diff, SwarmEventListener source) throws SwarmException {
        for (String fieldName : diff.getFieldNames()) {
            final JSONValue diffVal = diff.getFieldValue(fieldName);
            final SpecToken fieldVersion = new SpecToken(diffVal.getFieldValue(Field.VERSION).getValueAsStr());
            final JSONValue fieldValue = diffVal.getFieldValue(Field.VALUE);
            final String diffBare = fieldVersion.getBare();

            final Field field = this.getChild(new SpecToken(fieldName));
            if (field == null) { continue; }

            String currentBare = field.getVersion().getBare();
            if (diffBare.compareTo(currentBare) > 0) {
                field.set(field.getSpec().overrideToken(SpecQuant.VERSION, fieldVersion), fieldValue, source);
            }
        }
    }

    public void init(Set<Type.FieldDescription> fieldDescriptions, Spec objSpec, JSONValue fieldValues) throws SwarmException {
        for (Type.FieldDescription descr: fieldDescriptions) {
            Spec fieldSpec = getSpec().overrideToken(SpecQuant.MEMBER, descr.getName());
            Field fld = new Field(swarm, fieldSpec, descr);
            addField(fld);
            JSONValue fieldValue = fieldValues.getFieldValue(descr.getName().toString());
            if (fieldValue == null) {
                fieldValue = descr.getDefaultValue();
            }
            fld.init(fieldValue);
        }
    }

    private JSONValue getDiff(JSONValue base) throws SwarmException {
        Map<String, JSONValue> fields = new HashMap<String, JSONValue>();
        for (SpecToken fieldId : getChildrenKeys()) {
            final Field field = getChild(fieldId);
            SpecToken version = field.getVersion();
            JSONValue maxKnownVersion = base.getFieldValue(version.getExt());
            if (version.getBare().compareTo(maxKnownVersion.getValueAsStr()) > 0) {
                fields.put(fieldId.toString(), field.getValue());
            }
        }
        try {
            return new JSONValue(fields);
        } catch (JSONException e) {
            throw new SwarmException("error building json for diff: " + e.getMessage(), e);
        }
    }

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
