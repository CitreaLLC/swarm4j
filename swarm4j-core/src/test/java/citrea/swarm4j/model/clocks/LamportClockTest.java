package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.SpecQuant;
import citrea.swarm4j.model.spec.SpecToken;
import org.junit.Test;

import java.util.Arrays;

import static citrea.swarm4j.model.spec.SpecQuant.VERSION;
import static org.junit.Assert.*;

public class LamportClockTest {
    public static final String PROCESS_ID = "swarm~0";
    public static final String ZERO_TIME = "00000";

    @Test
    public void testIssueTimestamp() throws Exception {
        final Clock clock = new LamportClock(PROCESS_ID, ZERO_TIME);
        final int last = 5;
        assertEquals(
                "initialized correctly",
                new SpecToken(VERSION, ZERO_TIME, PROCESS_ID),
                clock.getLastIssuedTimestamp()
        );
        for (int i = 1; i <= last; i++) {
            assertEquals(
                    "increment(" + i + ")",
                    new SpecToken(VERSION, "0000" + i, PROCESS_ID),
                    clock.issueTimestamp()
            );
        }
        assertEquals(
                "lastIssued ok",
                new SpecToken(VERSION, "0000" + last, PROCESS_ID),
                clock.getLastIssuedTimestamp()
        );
    }

    @Test
    public void testParseTimestamp() throws Exception {
        final Clock clock = new LamportClock(PROCESS_ID, ZERO_TIME);
        for (int i = 1; i <= 5; i++) {
            SpecToken ts = new SpecToken(VERSION, "0000" + i, PROCESS_ID);
            TimestampParsed tsParsed = clock.parseTimestamp(ts);
            assertEquals("parse(" + i + ")", new TimestampParsed(0, i), tsParsed);
        }
    }

    @Test
    public void testSeeTimestamp() throws Exception {
        final Clock clock = new LamportClock(PROCESS_ID, ZERO_TIME);
        for (int i = 1; i <= 4; i++) {
            clock.seeTimestamp(new SpecToken(VERSION, "0000" + (i * 2), PROCESS_ID));
            assertEquals(
                    "see("+i+")",
                    new SpecToken(VERSION, "0000" + (i * 2 + 1), PROCESS_ID),
                    clock.issueTimestamp()
            );
        }
    }
}