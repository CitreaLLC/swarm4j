package citrea.swarm4j.model.spec;

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
        assertEquals(SpecQuant.ID, SpecQuant.VERSION.prev());
        assertEquals(SpecQuant.VERSION, SpecQuant.OP.prev());
    }

    @Test
    public void testNext() throws Exception {
        assertEquals(SpecQuant.ID, SpecQuant.TYPE.next());
        assertEquals(SpecQuant.VERSION, SpecQuant.ID.next());
        assertEquals(SpecQuant.OP, SpecQuant.VERSION.next());
        assertNull(SpecQuant.OP.next());
    }

    @Test
    public void testByCode() throws Exception {
        assertEquals(SpecQuant.TYPE, SpecQuant.byCode('/'));
        assertEquals(SpecQuant.ID, SpecQuant.byCode('#'));
        assertEquals(SpecQuant.OP, SpecQuant.byCode('.'));
        assertEquals(SpecQuant.VERSION, SpecQuant.byCode('!'));
        assertNull(SpecQuant.byCode('a'));
    }

    @Test
    public void testByOrder() throws Exception {
        assertEquals(SpecQuant.TYPE, SpecQuant.byOrder(0));
        assertEquals(SpecQuant.ID, SpecQuant.byOrder(1));
        assertEquals(SpecQuant.VERSION, SpecQuant.byOrder(2));
        assertEquals(SpecQuant.OP, SpecQuant.byOrder(3));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testByOrderWrong() throws Exception {
        SpecQuant.byOrder(4);
    }
}
