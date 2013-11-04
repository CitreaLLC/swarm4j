package citrea.swarm4j.model;

import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecQuant;
import citrea.swarm4j.spec.SpecToken;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 29/10/13
 *         Time: 01:13
 */
public class Field extends AbstractEventRelay implements EventRecipient {

    private Type.FieldDescription description;
    private SpecToken version;
    private JSONValue value;

    protected Field(Swarm swarm, Spec fieldName, Type.FieldDescription description) {
        super(swarm, fieldName);
        this.description = description;
    }

    public void init(SpecToken id, JSONValue value) throws SwarmException {
        logger.trace("init version={} value={}", id, value);
        this.value = value;
        this.version = id;
    }

    @Override
    protected AbstractEventRelay createNewChild(Spec spec, JSONValue value) {
        return null;
    }

    @Override
    protected void validate(Spec spec, JSONValue value, EventRecipient source) throws SwarmValidationException {
        SwarmValidator validator = description.getValidator();
        if (validator != null) {
            if (!validator.validate(spec, value, source)) {
                throw new SwarmValidationException(spec, "field");
            }
        }
    }

    @Override
    public void on(Action action, Spec spec, JSONValue value, EventRecipient source) throws SwarmException {
        throw new SwarmUnsupportedActionException(action);
    }

    @Override
    public void off(Action action, Spec spec, EventRecipient source) throws SwarmException {
        throw new SwarmUnsupportedActionException(action);
    }

    @Override
    public void set(Spec spec, JSONValue value, EventRecipient listener) throws SwarmException {

        SpecToken version = spec.getVersion();
        if (version == null) {
            version = swarm.newVersion();
            spec = spec.overrideToken(SpecQuant.VERSION, version);
        }

        this.value = value;
        this.version = version;
    }

    public SpecToken getVersion() {
        return version;
    }

    public JSONValue getValue() {
        return value;
    }
}
