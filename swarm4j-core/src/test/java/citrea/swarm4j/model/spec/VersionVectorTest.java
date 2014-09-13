package citrea.swarm4j.model.spec;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 26.08.2014
 *         Time: 16:48
 */
public class VersionVectorTest {

    @Test
    public void testAdd() throws Exception {
        Spec spec = new Spec("/Type!ver1+s1!ver2+s2!ver3+s1");
        VersionVector map = new VersionVector(spec);
        assertEquals("!ver3+s1!ver2+s2", map.toString());
    }

    @Test
    public void testAdd1() throws Exception {

    }

    @Test
    public void testCovers() throws Exception {
        // the convention is: use "!version" for vectors and
        // simply "version" for scalars
        VersionVector map = new VersionVector("!7AM0f+gritzko!0longago+krdkv!7AMTc+aleksisha!0ld!00ld#some+garbage");

        assertTrue(map.covers(new SpecToken("!7AM0f+gritzko")));
        assertFalse(map.covers(new SpecToken("!7AMTd+aleksisha")));
        assertFalse(map.covers(new SpecToken("!6AMTd+maxmaxmax")));
        assertTrue(map.covers(new SpecToken("!0ld")));
        assertFalse(map.covers(new SpecToken("!0le")));

    }

    @Test
    public void testGet() throws Exception {
        VersionVector map = new VersionVector("!7AM0f+gritzko!0longago+krdkv!7AMTc+aleksisha!0ld!00ld#some+garbage");

        assertEquals("0ld", map.get(SpecToken.NO_AUTHOR));
        assertEquals("", map.get("garbage"));
    }

    @Test
    public void testMaxTs() throws Exception {
        VersionVector map = new VersionVector("!7AM0f+gritzko!0longago+krdkv!7AMTc+aleksisha!0ld!00ld#some+garbage");

        assertEquals("7AMTc", map.maxTs());
    }

    @Test
    public void testToString() throws Exception {
        VersionVector map = new VersionVector("!7AM0f+gritzko!0longago+krdkv!7AMTc+aleksisha!0ld!00ld#some+garbage");

        assertEquals("!7AMTc+aleksisha!7AM0f+gritzko", map.toString(10, "6"));
        assertEquals("!7AMTc+aleksisha", map.toString(1, "6"));
    }
}
