package citrea.swarm4j.model.reflection;

import citrea.swarm4j.model.value.JSONValue;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.meta.FieldMeta;
import org.json.JSONException;

import java.lang.reflect.Field;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 10:20
 */
public class FieldWrapper implements FieldMeta {

    private Field field;


    public FieldWrapper(Field field) {
        this.field = field;
    }

    @Override
    public void set(Syncable object, JSONValue value) throws SwarmException {
        //TODO JSONValue <-> raw-value converter
        Class<?> type = field.getType();
        try {
            if (type.isAssignableFrom(JSONValue.class)) {
                field.set(object, value);
            } else if (Number.class.isAssignableFrom(type)) {
                field.set(object, value.getValue());
            } else if (type.isPrimitive()) {
                field.set(object, value.getValue());
            } else if (type.isEnum()) {
                // TODO what is right way to find Enum item?
                for (Object item : type.getEnumConstants()) {
                    if (item.equals(value.getValueAsStr())) {
                        field.set(object, item);
                        break;
                    }
                }
            } else {
                throw new SwarmException("Unsupported field type: " + type.getSimpleName());
            }
        } catch (IllegalAccessException e) {
            throw new SwarmException(e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return this.field.getName();
    }

    @Override
    public JSONValue get(Syncable object) throws SwarmException {
        try {
            Object rawValue = field.get(object);
            return JSONValue.convert(rawValue);
        } catch (IllegalAccessException e) {
            throw new SwarmException(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "FieldWrapper{" +
                "field=" + field.getName() +
                '}';
    }
}
