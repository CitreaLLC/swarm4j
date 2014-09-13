package citrea.swarm4j.model.pipe;

import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.spec.SpecToken;
import org.json.JSONException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02.09.2014
 *         Time: 22:36
 */
public class LoopbackConnection implements OpStream {

    private LoopbackConnection paired;
    private OpStreamListener sink;

    public LoopbackConnection() {
        this.paired = new LoopbackConnection(this);
    }

    public LoopbackConnection(LoopbackConnection paired) {
        this.paired = paired;
    }

    @Override
    public void setSink(OpStreamListener sink) {
        this.sink = sink;
    }

    @Override
    public void sendMessage(String message) {
        try {
            this.paired.sink.onMessage(message);
        } catch (SwarmException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (JSONException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        this.sink.onClose();
        this.paired.close();
    }

    public OpStream getPaired() {
        return paired;
    }
}
