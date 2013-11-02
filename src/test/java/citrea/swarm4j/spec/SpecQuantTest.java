package citrea.swarm4j.spec;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02/11/13
 *         Time: 23:26
 */
public class SpecQuantTest {
    @Test
    public void testPrev() throws Exception {
        assertNull(SpecQuant.TYPE.prev());
        assertEquals(SpecQuant.TYPE, SpecQuant.ID.prev());
        assertEquals(SpecQuant.ID, SpecQuant.MEMBER.prev());
        assertEquals(SpecQuant.MEMBER, SpecQuant.VERSION.prev());
    }

    @Test
    public void testNext() throws Exception {
        assertEquals(SpecQuant.ID, SpecQuant.TYPE.next());
        assertEquals(SpecQuant.MEMBER, SpecQuant.ID.next());
        assertEquals(SpecQuant.VERSION, SpecQuant.MEMBER.next());
        assertNull(SpecQuant.VERSION.next());
    }

    @Test
    public void testByCode() throws Exception {
        assertEquals(SpecQuant.TYPE, SpecQuant.byCode('/'));
        assertEquals(SpecQuant.ID, SpecQuant.byCode('#'));
        assertEquals(SpecQuant.MEMBER, SpecQuant.byCode('.'));
        assertEquals(SpecQuant.VERSION, SpecQuant.byCode('!'));
        assertNull(SpecQuant.byCode('a'));
    }

    @Test
    public void testByOrder() throws Exception {
        assertEquals(SpecQuant.TYPE, SpecQuant.byOrder(0));
        assertEquals(SpecQuant.ID, SpecQuant.byOrder(1));
        assertEquals(SpecQuant.MEMBER, SpecQuant.byOrder(2));
        assertEquals(SpecQuant.VERSION, SpecQuant.byOrder(3));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testByOrderWrong() throws Exception {
        SpecQuant.byOrder(4);
    }
}
