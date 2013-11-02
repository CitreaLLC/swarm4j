package citrea.swarm4j.spec;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 02/11/13
 *         Time: 23:44
 */
public class SpecTokenTest {

    @Test
    public void testDate2ts() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(SpecToken.EPOCH);
        assertEquals("00000", SpecToken.date2ts(calendar.getTime()));

        calendar.add(Calendar.SECOND, 1);
        assertEquals("00001", SpecToken.date2ts(calendar.getTime()));

        calendar.add(Calendar.SECOND, 63);
        assertEquals("00010", SpecToken.date2ts(calendar.getTime()));
    }

    @Test
    public void testInt2base() throws Exception {
        assertEquals("0000000010", SpecToken.int2base(64, 10));
        assertEquals("00010", SpecToken.int2base(64, 5));
        assertEquals("00011", SpecToken.int2base(65, 5));
    }

    @Test
    public void testGetBare() throws Exception {
        //token w/o ext part
        SpecToken tok = new SpecToken("simple");
        assertEquals("simple", tok.getBare());

        //token with ext part
        tok = new SpecToken("bare+ext");
        assertEquals("bare", tok.getBare());
    }

    @Test
    public void testGetExt() throws Exception {
        //token w/o ext part
        SpecToken tok = new SpecToken("simple");
        assertEquals("special <no_author> value when token w/o ext", SpecToken.NO_AUTHOR, tok.getExt());

        //token with ext part
        tok = new SpecToken("bare+ext");
        assertEquals("ext", tok.getExt());
    }

    @Test
    public void testJoiningConstructor() throws Exception {
        SpecToken tok = new SpecToken("bare1", "ext");
        assertEquals("produce correct token", "bare1+ext", tok.toString());
        assertEquals("bare1", tok.getBare());
        assertEquals("ext", tok.getExt());
    }

    @Test
    public void testOverrideBare() throws Exception {
        SpecToken tok = new SpecToken("bare1+ext");
        SpecToken tok2 = tok.overrideBare("bare2");
        assertNotSame("generates new SpecToken instance", tok, tok2);
        assertEquals("do not modify the object", "bare1+ext", tok.toString());
        assertEquals("do not modify object bare", "bare1", tok.getBare());
        assertEquals("do not modify object ext", "ext", tok.getExt());
        assertEquals("produce correct token", "bare2+ext", tok2.toString());
    }

    @Test
    public void testOverrideExt() throws Exception {
        SpecToken tok = new SpecToken("bare1+ext1");
        SpecToken tok2 = tok.overrideExt("ext2");
        assertNotSame("generates new SpecToken instance", tok, tok2);
        assertEquals("do not modify the object", "bare1+ext1", tok.toString());
        assertEquals("do not modify object bare", "bare1", tok.getBare());
        assertEquals("do not modify object ext", "ext1", tok.getExt());
        assertEquals("produce correct token", "bare1+ext2", tok2.toString());
    }

    @Test
    public void testWithQuant() throws Exception {
        SpecToken tok = new SpecToken("bare+ext");
        assertEquals("/bare+ext", tok.withQuant(SpecQuant.TYPE));
        assertEquals("#bare+ext", tok.withQuant(SpecQuant.ID));
        assertEquals(".bare+ext", tok.withQuant(SpecQuant.MEMBER));
        assertEquals("!bare+ext", tok.withQuant(SpecQuant.VERSION));
    }

    @Test
    public void testEquals() throws Exception {
        SpecToken tok = new SpecToken("bare+ext");

        SpecToken tokEq = new SpecToken("bare+ext");
        assertEquals(tokEq, tok);

        SpecToken tokNotEq = new SpecToken("bare2+ext");
        assertFalse(tok.equals(tokNotEq));

        //noinspection EqualsBetweenInconvertibleTypes
        assertTrue("comparable with strings", tok.equals("bare+ext"));
    }
}
