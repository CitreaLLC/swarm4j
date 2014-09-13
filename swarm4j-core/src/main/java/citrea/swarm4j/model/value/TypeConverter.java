package citrea.swarm4j.model.value;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 24.08.2014
 *         Time: 20:51
 */
public interface TypeConverter<T> {

    JSONValue toJSONValue(T value);
    T fromJSONValue(JSONValue value);
}
