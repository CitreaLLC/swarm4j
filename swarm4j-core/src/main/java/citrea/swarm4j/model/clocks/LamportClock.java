package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.SpecQuant;
import citrea.swarm4j.model.spec.SpecToken;

import java.util.Date;

import static citrea.swarm4j.model.spec.SpecQuant.VERSION;

/**
 * Pure logical-time Lamport clocks.
 *
 * @author aleksisha
 *         Date: 13.09.2014
 *         Time: 14:37
 */
public class LamportClock extends AbstractClock {

    public static final int SEQUENCE_PART_LENGTH = 5;

    public LamportClock(String processId, String initialTime) {
        super(processId, initialTime, 0);

        SpecToken specToken = new SpecToken(SpecQuant.VERSION, initialTime, processId);
        // sometimes we assume our local clock has some offset
        if (NO_INITIAL_TIME.equals(specToken.getBare())) {
            this.lastSeqSeen = -1;
            specToken = new SpecToken(VERSION, issueTimePart() + generateNextSequencePart(), id);
        }
        this.lastIssuedTimestamp = specToken;

        this.seeTimestamp(this.lastIssuedTimestamp);
    }

    public LamportClock(String processId) {
        this(processId, NO_INITIAL_TIME);
    }

    @Override
    protected String issueTimePart() {
        return "";
    }

    @Override
    protected String generateNextSequencePart() {
        int seq = ++this.lastSeqSeen;
        return SpecToken.int2base(seq, SEQUENCE_PART_LENGTH);
    }

    @Override
    protected int parseSequencePart(String seq) {
        return SpecToken.base2int(seq);
    }

    @Override
    public Date timestamp2date(SpecToken ts) {
        throw new UnsupportedOperationException("Lamport timestamp can't be converted to Date");
    }
}
