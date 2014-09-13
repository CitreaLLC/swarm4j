package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.SpecToken;
import org.junit.Test;

import static citrea.swarm4j.model.spec.SpecQuant.VERSION;
import static org.junit.Assert.assertEquals;

public class SecondPreciseClockTest {

    private static final String PROCESS_ID = "swarm~0";

    @Test
    public void testIssueTimestamp() throws Exception {
        final long initialTime = 0L;
        final FakeSecondPreciseClock clock = new FakeSecondPreciseClock(PROCESS_ID, initialTime);
        final int last = 5;
        assertEquals(
                "initialized correctly",
                new SpecToken(VERSION, "00000", PROCESS_ID),
                clock.getLastIssuedTimestamp()
        );
        for (int i = 1; i <= last; i++) {
            clock.tick();
            assertEquals(
                    "increment(" + i + ")",
                    new SpecToken(VERSION, "0000" + i, PROCESS_ID),
                    clock.issueTimestamp()
            );
        }
        for (int i = 1; i <= last; i++) {
            assertEquals(
                    "increment(" + i + ")",
                    new SpecToken(VERSION, "0000" + last + "0" + i, PROCESS_ID),
                    clock.issueTimestamp()
            );
        }
        assertEquals(
                "lastIssued ok",
                new SpecToken(VERSION, "0000" + last + "0" + last, PROCESS_ID),
                clock.getLastIssuedTimestamp()
        );
    }

    @Test
    public void testParseTimestamp() throws Exception {
        final long initialTime = 0L;
        final FakeSecondPreciseClock clock = new FakeSecondPreciseClock(PROCESS_ID, initialTime);
        for (int i = 1; i <= 5; i++) {
            SpecToken ts = new SpecToken(VERSION, "0000" + i, PROCESS_ID);
            TimestampParsed tsParsed = clock.parseTimestamp(ts);
            assertEquals("parse(" + i + ", 0)", new TimestampParsed(i, 0), tsParsed);

            for (int j = 1; j <= 5; j++) {
                ts = new SpecToken(VERSION, "0000" + i + "0" + j, PROCESS_ID);
                tsParsed = clock.parseTimestamp(ts);
                assertEquals("parse(" + i + ", " + j + ")", new TimestampParsed(i, j), tsParsed);
            }
        }
    }

    @Test
    public void testSeeTimestamp() throws Exception {
        final long initialTime = 0L;
        final FakeSecondPreciseClock clock = new FakeSecondPreciseClock(PROCESS_ID, initialTime);

        for (int i = 1; i <= 4; i++) {
            clock.seeTimestamp(new SpecToken(VERSION, "0000" + (i * 2), PROCESS_ID));
            assertEquals(
                    "see("+i+")",
                    new SpecToken(VERSION, "0000" + (i * 2) + "01", PROCESS_ID),
                    clock.issueTimestamp()
            );

            clock.seeTimestamp(new SpecToken(VERSION, "0000" + (i * 2) + "02", PROCESS_ID));
            assertEquals(
                    "see(" + i + ")",
                    new SpecToken(VERSION, "0000" + (i * 2) + "03", PROCESS_ID),
                    clock.issueTimestamp()
            );
        }

    }

    public static class FakeSecondPreciseClock extends SecondPreciseClock {

        private long currentTime = 0L;

        public FakeSecondPreciseClock(String processId, long currentTime) {
            super(processId);
            this.currentTime = currentTime;
        }

        public void tick() {
            this.currentTime += MILLIS_IN_SECOND;
        }

        @Override
        protected long getTimeInMillis() {
            return currentTime;
        }
    }
}