package citrea.swarm4j.model;

import org.json.JSONString;

/**
 * Used for field values storing
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 27/10/13
 *         Time: 16:03
 */
public class JSONValue implements JSONString {
    private String str;

    public JSONValue(String str) {
        this.str = str;
    }

    @Override
    public String toJSONString() {
        return str;
    }

    public static JSONValue valueOf(Object value) {
        JSONValue res = null;
        if (value != null) {
            res = new JSONValue(value.toString());
        }
        return res;
    }
}
