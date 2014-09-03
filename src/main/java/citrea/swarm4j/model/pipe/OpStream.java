package citrea.swarm4j.model.pipe;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 17:58
 */
public interface OpStream {

    void setSink(OpStreamListener sink);
    void sendMessage(String message);

    void close();
}
