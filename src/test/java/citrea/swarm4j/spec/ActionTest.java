package citrea.swarm4j.spec;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03/11/13
 *         Time: 12:42
 */
public class ActionTest {
    @Test
    public void testByName() throws Exception {
        assertEquals(Action.on, Action.byName("on"));
        assertEquals(Action.reOn, Action.byName("reOn"));
        assertEquals(Action.once, Action.byName("once"));
        assertEquals(Action.off, Action.byName("off"));
        assertEquals(Action.reOff, Action.byName("reOff"));
        assertEquals(Action.set, Action.byName("set"));
        assertEquals("return 'set' when unknown name passed", Action.set, Action.byName("unknown"));
    }

    @Test
    public void testAsToken() throws Exception {
        assertEquals("*on", Action.on.asToken());
        assertEquals("*reOn", Action.reOn.asToken());
        assertEquals("*once", Action.once.asToken());
        assertEquals("*off", Action.off.asToken());
        assertEquals("*reOff", Action.reOff.asToken());
        assertEquals("'*set' should be omitted", "", Action.set.asToken());
    }
}
