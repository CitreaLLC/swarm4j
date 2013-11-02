package citrea.swarm4j.model;

import org.json.JSONString;

import java.util.Collections;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 31/10/13
 *         Time: 00:07
 */
public interface FieldValues extends JSONString {
    public final FieldValues EMPTY = new Empty();

    Map<String, JSONString> getFieldValues();
    JSONString get(String field);


    public final class Empty implements FieldValues {

        @Override
        public Map<String, JSONString> getFieldValues() {
            return Collections.emptyMap();
        }

        @Override
        public JSONString get(String field) {
            return null;
        }

        @Override
        public String toJSONString() {
            return "null";
        }
    }
}
