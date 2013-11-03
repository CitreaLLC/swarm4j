package citrea.swarm4j.model;

import citrea.swarm4j.spec.Action;
import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecToken;
import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03/11/13
 *         Time: 10:01
 */
public class SwarmTest {
    private Swarm swarm;

    @org.junit.Before
    public void setUp() throws Exception {
        this.swarm = new Swarm(new SpecToken("Swarm"), new SpecToken("swarm"));
    }

    @org.junit.After
    public void tearDown() throws Exception {
        this.swarm = null;
    }

    @org.junit.Test
    public void testNewVersion() throws Exception {
        SpecToken ver1 = this.swarm.newVersion();
        SpecToken ver2 = this.swarm.newVersion();
        assertNotEquals(ver1, ver2);
    }

    @org.junit.Test
    public void testFreezeThaw() throws Exception {
        this.swarm.freeze();
        SpecToken ver1 = this.swarm.newVersion();
        SpecToken ver2 = this.swarm.newVersion();
        assertEquals(ver1, ver2);

        this.swarm.thaw();
        SpecToken ver3 = this.swarm.newVersion();
        assertNotEquals(ver1, ver3);
    }


    @org.junit.Test
    public void testOn() throws Exception {
        Spec specSubscribe = new Spec("/Swarm#client");
        Spec expectedReply = new Spec(this.swarm.getTypeId(), this.swarm.getId());

        RememberingRecipient fakeSource = new RememberingRecipient();
        this.swarm.on(Action.on, specSubscribe, new JSONValue("clientTs"), fakeSource);
        assertNull(fakeSource.lastSetParams);
        assertNull(fakeSource.lastOffParams);
        RememberingRecipient.Triplet lastOn = fakeSource.lastOnParams;
        assertNotNull(lastOn);
        assertNotNull(lastOn.spec);
        assertEquals(expectedReply, lastOn.spec);
        assertNotNull(lastOn.value);
        assertEquals(swarm.getId().toString(), lastOn.value.getValueAsStr());
    }

    @org.junit.Test(expected = SwarmNoChildException.class)
    public void testOnWrong() throws Exception {
        Spec incorrectSubscription = new Spec("/SwarmWrong#swarm.on");
        this.swarm.on(Action.on, incorrectSubscription, new JSONValue("clientTs"), null);
    }

    @org.junit.Test
    public void testOff() throws Exception {

    }

    @org.junit.Test(expected = SwarmNoChildException.class)
    public void testSet() throws Exception {
        Spec spec = new Spec("/Type#id.field");
        this.swarm.set(spec, new JSONValue("some-value"), null);
    }

    @org.junit.Test
    public void testGetLastTs() throws Exception {

    }


}
