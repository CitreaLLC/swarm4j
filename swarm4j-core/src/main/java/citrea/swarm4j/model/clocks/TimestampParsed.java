package citrea.swarm4j.model.clocks;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 09.09.2014
 *         Time: 16:59
 */
public class TimestampParsed {
    int time;
    int seq;

    public TimestampParsed(int time, int seq) {
        this.time = time;
        this.seq = seq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimestampParsed)) return false;

        TimestampParsed that = (TimestampParsed) o;

        return seq == that.seq && time == that.time;
    }

    @Override
    public int hashCode() {
        int result = time;
        result = 31 * result + seq;
        return result;
    }

    @Override
    public String toString() {
        return "TimestampParsed{" +
                "time=" + time +
                ", seq=" + seq +
                '}';
    }
}
