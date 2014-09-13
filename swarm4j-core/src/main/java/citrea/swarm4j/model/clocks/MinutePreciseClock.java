package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.SpecToken;

/**
 * It is not always necessary to have second-precise timestamps.
 * Going with minute-precise allows to fit timestamp values
 * into 30 bits (5 base64, 2 unicode chars).
 * More importantly, such timestamps increase incrementally for
 * short bursts of events (e.g. user typing). That allows
 * for sequence-coding optimizations in LongSpec.
 * In case processes generate more than 64 events a minute,
 * which is not unlikely, the optimization fails as we add
 * 12-bit seq (2 base64, 1 unicode).
 *
 * @author aleksisha
 *         Date: 13.09.2014
 *         Time: 14:54
 */
public class MinutePreciseClock extends SomePreciseClock {
    public static final int MILLIS_IN_MINUTE = 1000 * 60;
    public static final int MAX_SEQ = 1 << (6 * 3);

    public MinutePreciseClock(String processId, String initialTime) {
        super(processId, initialTime, 4, MILLIS_IN_MINUTE);
    }

    public MinutePreciseClock(String processId) {
        this(processId, NO_INITIAL_TIME);
    }

    @Override
    protected String generateNextSequencePart() {
        int seq = ++this.lastSeqSeen;
        if (seq >= MAX_SEQ) {
            throw new IllegalStateException("max event freq is 4000Hz");
        }
        return seq < 64 ? SpecToken.int2base(seq, 1) : SpecToken.int2base(seq, 3);
    }

    @Override
    protected int parseSequencePart(String seq) {
        switch (seq.length()) {
            case 1:
            case 3:
                return SpecToken.base2int(seq);
            default:
                throw new IllegalArgumentException("sequence part must be of 1 or 3 characters length");
        }
    }
}
