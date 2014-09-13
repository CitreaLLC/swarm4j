package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.SpecToken;

import java.util.Date;

import static citrea.swarm4j.model.spec.SpecQuant.VERSION;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 13.09.2014
 *         Time: 14:55
 */
public abstract class SomePreciseClock extends AbstractClock {

    protected final int preciseInMillis;

    protected long clockOffsetMs;

    protected SomePreciseClock(String processId, String initialTime, int timePartLen, int preciseInMillis) {
        super(processId, initialTime, timePartLen);
        this.preciseInMillis = preciseInMillis;

        // sometimes we assume our local clock has some offset

        // although we try hard to use wall clock time, we must
        // obey Lamport logical clock rules, in particular our
        // timestamps must be greater than any other timestamps
        // previously seen

        if (NO_INITIAL_TIME.equals(initialTime)) {
            this.clockOffsetMs = 0;
            initialTime = issueTimePart() + generateNextSequencePart();
        }
        this.lastIssuedTimestamp = new SpecToken(VERSION, initialTime, id);
        this.clockOffsetMs = parseTimestamp(this.lastIssuedTimestamp).time - this.getTimeInMillis();
        this.seeTimestamp(this.lastIssuedTimestamp);
    }

    @Override
    protected String issueTimePart() {
        int res = this.getApproximateTime();
        if (this.lastTimeSeen > res) {
            res = this.lastTimeSeen;
        }
        if (res > this.lastTimeSeen) {
            this.lastSeqSeen = -1;
        }
        this.lastTimeSeen = res;

        return SpecToken.int2base(res, timePartLen);
    }

    @Override
    public Date timestamp2date(SpecToken ts) {
        TimestampParsed parsed = parseTimestamp(ts);
        long millis = parsed.time * preciseInMillis + EPOCH;
        return new Date(millis);
    }

    protected int getApproximateTime() {
        return (int) getTimeInMillis() / preciseInMillis;
    }

    protected long getTimeInMillis() {
        long millis = System.currentTimeMillis();
        millis -= EPOCH;
        millis += this.clockOffsetMs;
        return millis;
    }
}
