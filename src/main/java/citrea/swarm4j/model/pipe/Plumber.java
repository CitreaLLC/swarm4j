package citrea.swarm4j.model.pipe;

import citrea.swarm4j.model.SwarmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 08.09.2014
 *         Time: 20:08
 */
public final class Plumber implements Runnable {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private static final long keepAliveTimeout = 60000L;

    private Thread queueThread;
    final PriorityBlockingQueue<Event> events = new PriorityBlockingQueue<Event>();

    public void start() {
        new Thread(this, "plumber").start();
    }

    public void stop() {
        synchronized (this) {
            if (queueThread != null) {
                queueThread.interrupt();
            }
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            if (queueThread != null) {
                throw new IllegalStateException("Can be started only once");
            }
            queueThread = Thread.currentThread();
        }

        logger.info("started");

        // TODO optimize (utilizes too much CPU time)
        while (!queueThread.isInterrupted()) {
            try {
                Event event = events.take();
                long now = new Date().getTime();
                if (event.time <= now) {
                    event.run();
                } else {
                    events.put(event);
                    Thread.sleep(500); // TODO config
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        logger.info("finished");
    }

    @Override
    public String toString() {
        return "Plumber";
    }

    public void keepAlive(Pipe pipe) {
        long now = new Date().getTime();
        synchronized (events) {
            events.offer(new KeepAliveEvent(pipe, now + keepAliveTimeout >> 2)); // TODO + Math.random() * 100
        }
    }

    public void reconnect(Pipe pipe) {
        long now = new Date().getTime();
        synchronized (events) {
            events.offer(new ReconnectEvent(pipe, now + pipe.reconnectTimeout));
        }
    }

    private abstract class Event implements Comparable<Event> {
        protected Pipe pipe;
        protected long time;

        public Event(Pipe pipe, long time) {
            this.pipe = pipe;
            this.time = time;
        }

        public abstract void run();

        @Override
        public int compareTo(Event other) {
            long delta = time - other.time;
            if (delta == 0L) {
                return 0;
            } else {
                return delta < 0 ? -1 : 1;
            }
        }
    }

    private class KeepAliveEvent extends Event {

        public KeepAliveEvent(Pipe pipe, long time) {
            super(pipe, time);
        }

        @Override
        public void run() {
            if (Pipe.State.CLOSED.equals(pipe.state)) {
                return;
            }
            long now = new Date().getTime();
            long sinceRecv = now - pipe.lastRecvTS;
            long sinceSend = now - pipe.lastSendTS;

            if (sinceSend > keepAliveTimeout >> 1) {
                pipe.sendMessage("{}");
            }
            if (sinceRecv > keepAliveTimeout) {
                pipe.close("stream timeout");
            }
            pipe.plumber.keepAlive(pipe);
        }
    }

    private class ReconnectEvent extends Event {

        public ReconnectEvent(Pipe pipe, long time) {
            super(pipe, time);
        }

        @Override
        public void run() {
            if (pipe.uri != null) {
                // TODO double reconnection timeout
                try {
                    pipe.host.connect(pipe.uri, Math.min(pipe.reconnectTimeout << 1, 30000));
                } catch (SwarmException e) {
                    // TODO log exception
                }
            }
        }
    }
}
