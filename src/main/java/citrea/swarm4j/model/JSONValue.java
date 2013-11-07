package citrea.swarm4j.model;

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
public final class JSONValue implements JSONString {

    public static final JSONString NULL = new JSONValue((String) null);

    private enum Type {
        SIMPLE, OBJECT, ARRAY
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

    public JSONValue(Map<String, Object> fieldValues) throws JSONException {
        this.type = Type.OBJECT;
        this.map = new HashMap<String, JSONValue>(fieldValues.size());
        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            this.map.put(entry.getKey(), new JSONValue(entry.getValue()));
        }
    }

    public JSONValue(Object value) throws JSONException {
        super();
        Object wrapped = JSONObject.wrap(value);
        if (wrapped == null) {
            throw new JSONException("incorrect json");
        }
        if (wrapped == JSONObject.NULL || wrapped == value) {
            this.type = Type.SIMPLE;
            this.simpleValue = value;
            this.json = JSONObject.valueToString(value);
        } else {
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
            }
        }
    }

    public boolean isSimple() {
        return type == Type.SIMPLE;
    }

    public Set<String> getFieldNames() {
        return this.type == Type.OBJECT ? this.map.keySet() : Collections.<String>emptySet();
    }

    public JSONValue getFieldValue(String fieldName) {
        return this.type == Type.OBJECT ? this.map.get(fieldName) : null;
    }

    public Object getValue() {
        return this.type == Type.SIMPLE ? simpleValue : null;
    }

    public String getValueAsStr() {
        return (this.type == Type.SIMPLE && simpleValue != null) ? simpleValue.toString() : null;
    }

    public boolean isEmpty() {
        return (this.type == Type.SIMPLE && (simpleValue == null || simpleValue.equals(""))) ||
                (this.type == Type.OBJECT && this.map.isEmpty()) ||
                (this.type == Type.ARRAY && this.arr.isEmpty());
    }

    @Override
    public String toJSONString() {
        switch (type) {
            case SIMPLE:
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
}
