package citrea.swarm4j.model.hash;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03.09.2014
 *         Time: 23:37
 */
public class SimpleHash implements HashFunction {

    @Override
    public int calc(String value) {
        int hash = 5381;
        for (int i = 0, l = value.length(); i < l; i++) {
            hash = ((hash << 5) + hash) + value.charAt(i);
        }
        return hash;
    }
}
