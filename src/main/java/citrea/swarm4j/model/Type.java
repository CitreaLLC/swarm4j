package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecQuant;
import citrea.swarm4j.spec.SpecToken;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 29/10/13
 *         Time: 00:52
 */
public class Type extends AbstractEventRelay<Model> {

    private Set<FieldDescription> fieldDescriptions = new HashSet<FieldDescription>();

    public Type(Swarm swarm, Spec typeSpec) {
        super(swarm, typeSpec, SpecQuant.ID);
    }

    public void registerField(FieldDescription description) {
        if (fieldDescriptions.contains(description)) {
            throw new IllegalArgumentException("field already registered: " + description);
        }
        fieldDescriptions.add(description);
    }

    @Override
    protected Model createNewChild(Spec spec, JSONValue value) throws SwarmException {
        SpecToken id = spec.getId();
        Model res;
        if (id == null) { //need new id
            //for all initial field values got the same id
            swarm.freeze();
            try {
                id = swarm.newVersion();
                res = new Model(swarm, getSpec().overrideToken(SpecQuant.ID, id));
                res.init(fieldDescriptions, value);
            } finally {
                swarm.thaw();
            }
        }
        return new Model(swarm, getSpec().overrideToken(SpecQuant.ID, id));
    }

    public static class FieldDescription {
        private SpecToken name;
        private JSONValue defaultValue;
        private SwarmValidator validator;

        public FieldDescription(SpecToken name) {
            this(name, null, null);
        }

        public FieldDescription(SpecToken name, JSONValue defaultValue) {
            this(name, defaultValue, null);
        }

        public FieldDescription(SpecToken name, JSONValue defaultValue, SwarmValidator validator) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.validator = validator;
        }

        public SpecToken getName() {
            return name;
        }

        public String getNameAsStr() {
            return name == null ? null : name.toString();
        }

        public SwarmValidator getValidator() {
            return validator;
        }

        public JSONValue getDefaultValue() {
            return defaultValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FieldDescription that = (FieldDescription) o;

            if (!name.equals(that.name)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "FieldDescription{" +
                    "name=" + name +
                    '}';
        }
    }
}
