package citrea.swarm4j.model.spec;

import citrea.swarm4j.model.Syncable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03/11/13
 *         Time: 00:34
 */
public class SpecTest {

    @Test
    public void testOverrideToken() throws Exception {
        Spec spec, spec2;
        //shortening
        spec = new Spec("/Type#id!version.op");
        spec2 = spec.overrideToken(new SpecToken(SpecQuant.VERSION, "version2"));
        assertNotSame(spec, spec2);
        assertEquals("/Type#id!version2.op", spec2.toString());
        spec2 = spec.overrideToken(new SpecToken(SpecQuant.OP, "op2"));
        assertEquals("/Type#id!version.op2", spec2.toString());
        spec2 = spec.overrideToken(new SpecToken(SpecQuant.ID, "id2"));
        assertEquals("/Type#id2!version.op", spec2.toString());
        spec2 = spec.overrideToken(new SpecToken(SpecQuant.TYPE, "Type2"));
        assertEquals("/Type2#id!version.op", spec2.toString());

        //extending
        spec = new Spec("/Type");
        spec2 = spec.overrideToken(new SpecToken(SpecQuant.ID, "id"));
        assertEquals("/Type#id", spec2.toString());
    }

    @Test
    public void testIsEmpty() throws Exception {
        Spec emptySpec = new Spec();
        assertTrue(emptySpec.isEmpty());
    }

    @Test
    public void testGetTokensCount() throws Exception {
        Spec spec;
        spec = new Spec("/Type#id!version.operation");
        assertEquals(4, spec.getTokensCount());
        spec = new Spec("/Type#id!version");
        assertEquals(3, spec.getTokensCount());
        spec = new Spec("/Type#id");
        assertEquals(2, spec.getTokensCount());
        spec = new Spec("!version.operation");
        assertEquals(2, spec.getTokensCount());
        spec = new Spec("/Type");
        assertEquals(1, spec.getTokensCount());
        spec = new Spec();
        assertEquals(0, spec.getTokensCount());
    }

    @Test
    public void testGetToken() throws Exception {
        Spec spec;
        spec = new Spec("/Type#id!version.op");
        assertEquals("/Type", spec.getToken(SpecQuant.TYPE).toString());
        assertEquals("#id", spec.getToken(SpecQuant.ID).toString());
        assertEquals(".op", spec.getToken(SpecQuant.OP).toString());
        assertEquals("!version", spec.getToken(SpecQuant.VERSION).toString());
    }

    @Test
    public void testGetTokenAliases() throws Exception {
        Spec spec = new Spec("/Type#id!version.op");
        assertSame(spec.getToken(SpecQuant.TYPE), spec.getType());
        assertSame(spec.getToken(SpecQuant.ID), spec.getId());
        assertSame(spec.getToken(SpecQuant.OP), spec.getOp());
        assertSame(spec.getToken(SpecQuant.VERSION), spec.getVersion());
    }

    @Test
    public void testGetTypeId() throws Exception {
        Spec spec = new Spec("/Type#id!version.op");
        assertEquals("/Type#id", spec.getTypeId().toString());
    }

    @Test
    public void testGetVersionOp() throws Exception {
        Spec spec = new Spec("/Type#id!version.op");
        assertEquals("!version.op", spec.getVersionOp().toString());
    }

    @Test
    public void testGetTokenIterator() throws Exception {
        Spec spec = new Spec("/Type1!v1+s1/Type2!v2+s2!v3+s2");
        Iterator<SpecToken> it = spec.getTokenIterator(SpecQuant.VERSION);
        List<SpecToken> tokens = new ArrayList<SpecToken>();
        while (it.hasNext()) {
            tokens.add(it.next());
        }
        assertEquals(3, tokens.size());
        assertEquals(new SpecToken("!v1+s1"), tokens.get(0));
        assertEquals(new SpecToken("!v2+s2"), tokens.get(1));
        assertEquals(new SpecToken("!v3+s2"), tokens.get(2));
    }

    @Test
    public void testSort() throws Exception {
        final String rightOrdered = "/Type#id!ver.op";
        Spec spec = new Spec(rightOrdered);
        // leave correct order
        assertEquals(rightOrdered, spec.sort().toString());
        // fix order
        spec = new Spec("#id/Type!ver.op");
        assertEquals(rightOrdered, spec.sort().toString());
        spec = new Spec("#id!ver/Type.op");
        assertEquals(rightOrdered, spec.sort().toString());
        spec = new Spec(".op!ver#id/Type");
        assertEquals(rightOrdered, spec.sort().toString());
    }

    @Test
    public void testToString() throws Exception {
        Spec spec = new Spec(
                new SpecToken("/Mouse"),
                new SpecToken("#s1"),
                new SpecToken("!8oJOb03+s1~0"),
                Syncable.ON
        );
        assertEquals("/Mouse#s1!8oJOb03+s1~0.on", spec.toString());
    }

    @Test
    public void testEquals() throws Exception {
        Spec spec = new Spec("/Mouse#s1!8oJOb03+s1~0.on");
        assertTrue("comparing to string", spec.equals("/Mouse#s1!8oJOb03+s1~0.on"));
    }
}
