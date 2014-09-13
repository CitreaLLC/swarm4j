package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.SpecToken;

/**
 * Swarm is based on the Lamport model of time and events in a
 * distributed system, so Lamport timestamps are essential to
 * its functioning. In most of the cases, it is useful to
 * use actuall wall clock time to create timestamps. This
 * class creates second-precise Lamport timestamps.
 * Timestamp ordering is alphanumeric, length may vary.
 *
 * @author aleksisha
 *         Date: 09.09.2014
 *         Time: 16:45
 */
public class SecondPreciseClock extends SomePreciseClock {
    public static final int MILLIS_IN_SECOND = 1000;
    public static final int MAX_SEQ = 1 << (6 * 2);
    public static final int TIME_PART_LEN = 5;

    /**
     *
     * @param processId id of the process/clock to add to every timestamp
     *        (like !timeseq+gritzko~ssn, where gritzko is the user
     *        and ssn is a session id, so processId is "gritzko~ssn").
     * @param initialTime normally, that is server-supplied timestamp
     *        to init our time offset; there is no guarantee about
     *        clock correctness on the client side
     */
    public SecondPreciseClock(String processId, String initialTime) {
        super(processId, initialTime, TIME_PART_LEN, MILLIS_IN_SECOND);
    }

    public SecondPreciseClock(String processId) {
        this(processId, NO_INITIAL_TIME);
    }

    @Override
    public String generateNextSequencePart() {
        int seq = ++this.lastSeqSeen;
        if (seq >= MAX_SEQ) {
            throw new IllegalStateException("max event freq is 4000Hz");
        }
        return seq > 0 ? SpecToken.int2base(seq, 2) : "";
    }

    @Override
    protected int parseSequencePart(String seq) {
        if (!seq.isEmpty() && seq.length() != 2) {
            throw new IllegalArgumentException("sequence part must be empty or 2 characters length (got: " + seq + ")");
        }
        return !seq.isEmpty() ? SpecToken.base2int(seq) : 0;
    }

}
