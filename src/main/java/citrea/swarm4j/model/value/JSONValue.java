package citrea.swarm4j.model.value;

import citrea.swarm4j.model.spec.SyncableRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;

import java.util.*;

/**
 * Used for field values storing
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 27/10/13
 *         Time: 16:03
 */
public final class JSONValue implements JSONString, Cloneable {

    public static final JSONValue NULL = new JSONValue((String) null);

    private enum Type {
        SIMPLE, OBJECT, ARRAY, REF
    }

    private Type type;
    private Map<String, JSONValue> map;
    private List<JSONValue> arr;
    private String json;
    private Object simpleValue;

    public JSONValue(String string) {
        this.type = Type.SIMPLE;
        this.simpleValue = string;
        this.json = string == null ? "null" : JSONObject.quote(string);
    }

    public JSONValue(boolean value) {
        this.type = Type.SIMPLE;
        this.simpleValue = value;
        this.json = value ? "true" : "false";
    }

    public JSONValue(Number value) {
        this.type = Type.SIMPLE;
        this.simpleValue = value;
        try {
            this.json = JSONObject.valueToString(value);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Error converting the given number to json");
        }
    }

    public JSONValue(Map<String, JSONValue> fieldValues) {
        this.type = Type.OBJECT;
        this.map = new HashMap<String, JSONValue>(fieldValues);
    }

    public JSONValue(SyncableRef value) {
        this.type = Type.REF;
        this.simpleValue = value;
        this.json = JSONObject.quote(value.toString());
    }

    public JSONValue(JSONValue toClone) {
        this.type = toClone.type;
        this.json = toClone.json;
        switch (this.type) {
            case ARRAY:
                this.arr = new ArrayList<JSONValue>(toClone.arr.size());
                for (JSONValue item : toClone.arr) {
                    this.arr.add(new JSONValue(item));
                }
                break;
            case OBJECT:
                this.map = new HashMap<String, JSONValue>(toClone.map.size());
                for (Map.Entry<String, JSONValue> entry : toClone.map.entrySet()) {
                    this.map.put(entry.getKey(), new JSONValue(entry.getValue()));
                }
                break;
            case SIMPLE:
            case REF:
                this.simpleValue = toClone.simpleValue;
                break;
            default:
                throw new IllegalArgumentException("Unknown JSONValue.Type: " + this.type);
        }
    }

    public JSONValue(Object value) throws JSONException {
        super();
        Object wrapped = JSONObject.wrap(value);
        if (wrapped == null) {
            throw new JSONException("incorrect json");
        }
        if (wrapped instanceof JSONObject) {
                this.type = Type.OBJECT;
                this.map = new HashMap<String, JSONValue>();
                JSONObject jo = ((JSONObject) wrapped);
                Iterator it = jo.keys();
                while (it.hasNext()) {
                    String key = String.valueOf(it.next());
                    this.map.put(key, new JSONValue(jo.get(key)));
                }
        } else if (wrapped instanceof JSONArray) {
            this.type = Type.ARRAY;
            this.arr = new ArrayList<JSONValue>();
            JSONArray ja = ((JSONArray) wrapped);
            for (int i = 0; i < ja.length(); i++) {
                 this.arr.add(new JSONValue(ja.get(i)));
            }
        } else if (wrapped == value || wrapped == JSONObject.NULL) {
            this.type = Type.SIMPLE;
            this.simpleValue = value;
            this.json = JSONObject.valueToString(value);
        }
    }

    public boolean isSimple() {
        return type == Type.SIMPLE;
    }

    public Set<String> getFieldNames() {
        return this.type == Type.OBJECT ? this.map.keySet() : Collections.<String>emptySet();
    }

    public JSONValue getFieldValue(String fieldName) {
        final JSONValue res;
        if (this.type == Type.OBJECT && this.map.containsKey(fieldName)) {
            res = this.map.get(fieldName);
        } else {
            res = JSONValue.NULL;
        }
        return res;
    }

    public void setFieldValue(String fieldName, JSONValue value) {
        if (this.type == Type.OBJECT) {
            this.map.put(fieldName, value);
        }
    }

    public void setFieldValue(String fieldName, Object value) throws JSONException {
        if (this.type == Type.OBJECT) {
            final JSONValue val;
            if (value instanceof JSONValue) {
                val = (JSONValue) value;
            } else {
                val = new JSONValue(value);
            }
            this.map.put(fieldName, val);
        }
    }

    public void removeFieldValue(String fieldName) {
        if (this.type == Type.OBJECT) {
            this.map.remove(fieldName);
        }
    }

    public Object getValue() {
        switch (this.type) {
            case SIMPLE:
            case REF:
                return simpleValue;
            case ARRAY:
                return arr;
            case OBJECT:
                return map;
            default:
                throw new IllegalStateException("Unsupported JSONValue type: " + this.type);
        }
    }

    public Map<String, JSONValue> getValueAsMap() {
        return this.type == Type.OBJECT ? map : Collections.<String, JSONValue>emptyMap();
    }

    public String getValueAsStr() {
        return (this.type == Type.SIMPLE && simpleValue != null) ? simpleValue.toString() : null;
    }

    public Integer getValueAsInteger() {
        return (this.type == Type.SIMPLE && simpleValue instanceof Number) ? ((Number) simpleValue).intValue() : null;
    }

    public boolean isEmpty() {
        return (this.type == Type.SIMPLE && (simpleValue == null || simpleValue.equals(""))) ||
                (this.type == Type.REF && (simpleValue == null)) ||
                (this.type == Type.OBJECT && this.map.isEmpty()) ||
                (this.type == Type.ARRAY && this.arr.isEmpty());
    }

    @Override
    public String toJSONString() {
        switch (type) {
            case SIMPLE:
            case REF:
                return json;
            case OBJECT:
                return new JSONObject(this.map).toString();
            case ARRAY:
                return new JSONObject(this.arr).toString();
            default:
                return null;
        }
    }

    public String toString() {
        return toJSONString();
    }

    @Override
    public JSONValue clone() {
        return new JSONValue(this);
    }
}
