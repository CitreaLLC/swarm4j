package citrea.swarm4j.spec;

import org.junit.Test;
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
    public void testBuildParent() throws Exception {
        Spec spec = new Spec("/Swarm#procId.on");
        Spec parent = spec.buildParent();
        assertNotSame("generates new Spec instance", spec, parent);
        assertEquals("/Swarm#procId", parent.toString());

        spec = new Spec("/Type#id.field!version").buildParent();
        assertEquals("/Type#id.field", spec.toString());

        spec = spec.buildParent();
        assertEquals("/Type#id", spec.toString());

        spec = spec.buildParent();
        assertEquals("/Type", spec.toString());

        spec = spec.buildParent();
        assertEquals("", spec.toString());
        assertTrue(spec.isEmpty());
    }

    @Test
    public void testOverrideToken() throws Exception {
        Spec spec, spec2;
        //shortening
        spec = new Spec("/Type#id.field!version");
        spec2 = spec.overrideToken(SpecQuant.VERSION, new SpecToken("version2"));
        assertNotSame(spec, spec2);
        assertEquals("/Type#id.field!version2", spec2.toString());
        spec2 = spec.overrideToken(SpecQuant.MEMBER, new SpecToken("field2"));
        assertEquals("/Type#id.field2", spec2.toString());
        spec2 = spec.overrideToken(SpecQuant.ID, new SpecToken("id2"));
        assertEquals("/Type#id2", spec2.toString());
        spec2 = spec.overrideToken(SpecQuant.TYPE, new SpecToken("Type2"));
        assertEquals("/Type2", spec2.toString());

        //extending
        spec = new Spec("/Type");
        spec2 = spec.overrideToken(SpecQuant.ID, new SpecToken("id"));
        assertEquals("/Type#id", spec2.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOverrideTokenWrong() throws Exception {
        Spec spec = new Spec("/Type");
        //skipping id is wrong
        spec.overrideToken(SpecQuant.MEMBER, new SpecToken("field"));
    }

    @Test
    public void testIsEmpty() throws Exception {
        Spec emptySpec = new Spec("/Type").buildParent();
        assertTrue(emptySpec.isEmpty());
    }

    @Test
    public void testGetTokensCount() throws Exception {
        Spec spec;
        spec = new Spec("/Type#id.field!version");
        assertEquals(4, spec.getTokensCount());
        spec = new Spec("/Type#id.field");
        assertEquals(3, spec.getTokensCount());
        spec = new Spec("/Type#id");
        assertEquals(2, spec.getTokensCount());
        spec = new Spec("/Type");
        assertEquals(1, spec.getTokensCount());
        assertEquals(0, spec.buildParent().getTokensCount());
    }

    @Test
    public void testGetToken() throws Exception {
        Spec spec;
        spec = new Spec("/Type#id.field!version");
        assertEquals("Type", spec.getToken(SpecQuant.TYPE).toString());
        assertEquals("id", spec.getToken(SpecQuant.ID).toString());
        assertEquals("field", spec.getToken(SpecQuant.MEMBER).toString());
        assertEquals("version", spec.getToken(SpecQuant.VERSION).toString());
    }

    @Test
    public void testGetLastToken() throws Exception {
        Spec spec = new Spec("/Type#id.field!version");
        assertEquals("version", spec.getLastToken().toString());
        spec = spec.buildParent();
        assertEquals("field", spec.getLastToken().toString());
        spec = spec.buildParent();
        assertEquals("id", spec.getLastToken().toString());
        spec = spec.buildParent();
        assertEquals("Type", spec.getLastToken().toString());
        spec = spec.buildParent();
        assertNull(spec.getLastToken());
    }

    @Test
    public void testGetTokenAliases() throws Exception {
        Spec spec = new Spec("/Type#id.field!version");
        assertSame(spec.getToken(SpecQuant.TYPE), spec.getType());
        assertSame(spec.getToken(SpecQuant.ID), spec.getId());
        assertSame(spec.getToken(SpecQuant.MEMBER), spec.getMember());
        assertSame(spec.getToken(SpecQuant.VERSION), spec.getVersion());
    }


    @Test
    public void testGetVersionAsStr() throws Exception {
        Spec spec = new Spec("/Type#id.field!version");
        assertEquals("version", spec.getVersionAsStr());
    }

    @Test
    public void testToString() throws Exception {
        //TODO test
    }

    @Test
    public void testEquals() throws Exception {
        //TODO test
    }
}
