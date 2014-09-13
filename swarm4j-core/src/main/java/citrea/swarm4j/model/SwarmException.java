package citrea.swarm4j.model;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 31/10/13
 *         Time: 17:21
 */
public class SwarmException extends Exception {

    public SwarmException(String s) {
        super(s);
    }

    public SwarmException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
