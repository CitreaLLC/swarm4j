package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecQuant;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;
import org.json.JSONException;
import org.json.JSONTokener;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 03/11/13
 *         Time: 10:01
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HostTest {
    public static Logger logger = LoggerFactory.getLogger(HostTest.class);

    private Thread storageThread;
    private XInMemoryStorage storage;
    private Thread hostThread;
    private Host host;

    @Before
    public void setUp() throws Exception {
        storage = new XInMemoryStorage(new SpecToken("dummy"));
        storageThread = new Thread(storage);
        storageThread.start();
        host = new Host(new SpecToken("#gritzko"), storage);
        host.registerType(Duck.class);
        hostThread = new Thread(host);
        hostThread.start();
        while (!host.ready()) {
            Thread.sleep(10);
        }
    }

    @After
    public void tearDown() throws Exception {
        host.close();
        host = null;
        hostThread.interrupt();
        hostThread = null;

        storage = null;
        storageThread.interrupt();
        storageThread = null;
    }

    @Test
    public void testNewVersion() throws Exception {
        SpecToken ver1 = new SpecToken(SpecQuant.VERSION, this.host.time());
        SpecToken ver2 = new SpecToken(SpecQuant.VERSION, this.host.time());
        assertNotEquals(ver1, ver2);
    }

    @Test
    public void test2a_basic_listener() throws Exception {
        logger.info("2.a basic source func");
        // expect(5);

        // construct an object with an id provided; it will try to fetch
        // previously saved state for the id (which is none)
        final Duck huey = host.get(new Spec("/Duck#hueyA"));
        Thread.sleep(10); // wait for initialization

        final int[] invoked = new int[] {0, 0};

        //ok(!huey._version); //storage is a?sync
        // listen to a field
        final OpRecipient lsfn2a = new OpRecipient() {
            @Override
            public void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
                // check spec
                Spec expectedSpec = new Spec(
                        new SpecToken("/Duck"),
                        new SpecToken("#hueyA"),
                        spec.getVersion(),
                        Model.SET
                );
                assertEquals(expectedSpec, spec);
                // check value
                JSONValue age = value.getFieldValue("age");
                assertNotNull(age);
                assertFalse(age.isEmpty());
                assertEquals(1, age.getValueAsInteger().intValue());

                // check version
                SpecToken version = spec.getVersion();
                assertEquals("gritzko", version.getExt());

                huey.off(this);
                // only the uplink remains and no listeners
                assertEquals(1, huey.uplinks.size());
                assertEquals(0, huey.listeners.size());
                invoked[1]++;
            }
        };

        final OpRecipient init2a = new OpRecipient() {
            @Override
            public void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
                JSONValue fieldValues = new JSONValue(new HashMap<String, JSONValue>());
                try {
                    fieldValues.setFieldValue("age", 1);
                } catch (JSONException e) {
                    throw new SwarmException(e.getMessage(), e);
                }
                huey.set(fieldValues);
                invoked[0]++;
            }
        };

        huey.on(huey.newEventSpec(Syncable.ON), new JSONValue("age"), lsfn2a);
        huey.on(huey.newEventSpec(Syncable.ON), new JSONValue(".init"), init2a);

        assertEquals(1, invoked[0]);
        assertEquals(1, invoked[1]);
    }

    @Test
    public void test2b_create_by_id() throws Exception {
        logger.info("2.b create-by-id");
        // there is 1:1 spec-to-object correspondence;
        // an attempt of creating a second copy of a model object
        // will throw an exception
        Duck dewey1 = new Duck(new SpecToken("#dewey"), host);
        // that's we resort to descendant() doing find-or-create
        Duck dewey2 = host.get(new Spec("/Duck#dewey"));
        // must be the same object
        assertSame(dewey1, dewey2);
        assertEquals(Duck.class.getSimpleName(), dewey1.getType().getBody());
    }

    @Test
    public void test2c_version_ids() throws Exception {
        logger.info("2.c version ids");
        String ts1 = host.time();
        Duck louie = host.get(new Spec("/Duck#louie"));
        louie.set(new JSONValue(Collections.singletonMap("age", new JSONValue(3))));
        Thread.sleep(10);
        String ts2 = host.time();
        assertTrue(ts1.compareTo(ts2) < 0);
        assertNotNull(louie.version);
        String vid = louie.version.substring(1);
        assertTrue(ts1.compareTo(vid) < 0);
        assertTrue(ts2.compareTo(vid) > 0);
    }

    @Test
    public void test2d_pojos() throws Exception {
        logger.info("2.d pojos");
        Duck dewey = host.get(new Spec("/Duck"));
        dewey.set(new JSONValue(Collections.singletonMap("age", new JSONValue(2))));
        JSONValue json = dewey.getPOJO(false);
        assertEquals("{\"height\":null,\"age\":2,\"mood\":\"neutral\"}", json.toJSONString());
    }

    /* TODO reactions
    @Test
    public void test2e_reactions() throws Exception {
        logger.info("2.e reactions");
        Duck huey = host.get("/Duck#huey");
        var handle = Duck.addReaction('age', function reactionFn(spec,val) {
            logger.debug("yupee im growing");
            assertEquals(val.age,1);
            start();
        });
        //var version = host.time(), sp = '!'+version+'.set';
        huey.deliver(huey.newEventSpec('set'), {age:1});
        Duck.removeReaction(handle);
        assertEquals(Duck.prototype._reactions['set'].length,0); // no house cleaning :)
    }

    */
    @Test
    public void test2f_once() throws Exception {
        logger.info("2.f once");
        Duck huey = host.get(new Spec("/Duck#huey"));
        Thread.sleep(10);

        RememberingRecipient listener = new RememberingRecipient();
        huey.once(new JSONValue("age"), listener);
        huey.set(new JSONValue(Collections.singletonMap("age", new JSONValue(4))));
        huey.set(new JSONValue(Collections.singletonMap("age", new JSONValue(5))));
        List<RememberingRecipient.Triplet> invocations = listener.getMemory();
        assertEquals(1, invocations.size());

        huey.set(new JSONValue(Collections.singletonMap("age", new JSONValue(6))));
        Thread.sleep(10);

        invocations = listener.getMemory();
        assertEquals(1, invocations.size());
    }

    /* TODO custom field types
    test('2.g custom field type',function (test) {
        console.warn(QUnit.config.current.testName);
        env.localhost= host;
        var huey = host.get('/Duck#huey');
        huey.set({height:'32cm'});
        ok(Math.abs(huey.height.meters-0.32)<0.0001);
        var vid = host.time();
        host.deliver(new Spec('/Duck#huey!'+vid+'.set'),{height:'35cm'});
        ok(Math.abs(huey.height.meters-0.35)<0.0001);
    });
    */

    @Test
    public void test2h_state_init() throws Exception {
        logger.info("2.h state init");
        JSONTokener t = new JSONTokener("{\"age\":1,\"height\":4}");
        JSONValue initialState = JSONValue.convert(t.nextValue());
        Duck factoryBorn = new Duck(initialState, host);
        Thread.sleep(10);
        assertEquals(4, factoryBorn.height.intValue());
        assertEquals(1, factoryBorn.age.intValue());
    }

    @Test
    public void test2i_batched_set() throws Exception {
        logger.info("2.i batched set");
        JSONTokener t = new JSONTokener("{\"age\":2,\"height\":5}");
        JSONValue fieldValues = JSONValue.convert(t.nextValue());
        Duck nameless = new Duck(host);
        nameless.set(fieldValues);
        assertEquals(2, nameless.age.intValue());
        assertEquals(5, nameless.height.intValue());
        assertFalse(nameless.canDrink());
    }

    /* TODO Sets
    @Test
    public void test2j_basic_Set_functions() throws Exception {
        logger.info("2.j basic Set functions (string index)");
        var hueyClone = new Duck({age:2});
        var deweyClone = new Duck({age:1});
        var louieClone = new Duck({age:3});
        var clones = new Nest();
        clones.addObject(louieClone);
        clones.addObject(hueyClone);
        clones.addObject(deweyClone);
        var sibs = clones.list(function(a,b){return a.age - b.age;});
        strictEqual(sibs[0],deweyClone);
        strictEqual(sibs[1],hueyClone);
        strictEqual(sibs[2],louieClone);
        var change = {};
        change[hueyClone.spec()] = 0;
        clones.change(change);
        var sibs2 = clones.list(function(a,b){return a.age - b.age;});
        equal(sibs2.length,2);
        strictEqual(sibs2[0],deweyClone);
        strictEqual(sibs2[1],louieClone);
    });*/

    @Test
    public void test2k_distilled_log() throws Exception {
        logger.info("2.k distilled log");
        JSONValue fieldValues = new JSONValue(new HashMap<String, JSONValue>());

        Duck duckling1 = host.get(Duck.class);
        Thread.sleep(10);//wait for init

        fieldValues.setFieldValue("age", 1);
        duckling1.set(fieldValues);

        fieldValues.setFieldValue("age", 2);
        duckling1.set(fieldValues);

        duckling1.distillLog();
        assertEquals(1, duckling1.oplog.size());

        fieldValues.setFieldValue("age", 3);
        fieldValues.setFieldValue("height", 30);
        duckling1.set(fieldValues);

        fieldValues.setFieldValue("age", 4);
        fieldValues.setFieldValue("height", 40);
        duckling1.set(fieldValues);

        duckling1.distillLog();
        assertEquals(1, duckling1.oplog.size());

        fieldValues.setFieldValue("age", 5);
        fieldValues.removeFieldValue("height");
        duckling1.set(fieldValues);

        duckling1.distillLog();

        assertEquals(2, duckling1.oplog.size());
    }

    @Test
    public void test2l_patial_order() throws Exception {
        logger.info("2.l partial order");
        Duck duckling = new Duck(host);
        JSONValue fieldValues = new JSONValue(new HashMap<String, JSONValue>());

        Spec spec1 = duckling.getTypeId().addToken("!time+user2").addToken(Model.SET).sort();
        fieldValues.setFieldValue("height", 2);
        duckling.deliver(spec1, fieldValues, OpRecipient.NOOP);

        Spec spec2 = duckling.getTypeId().addToken("!time+user1").addToken(Model.SET).sort();
        fieldValues.setFieldValue("height", 1);
        duckling.deliver(spec2, fieldValues, OpRecipient.NOOP);

        Thread.sleep(10);

        assertEquals(2, duckling.height.intValue());
    }

    @Test
    public void test2m_init_push() throws Exception {
        logger.info("2.m init push");
        JSONValue fieldValues = new JSONValue(new HashMap<String, JSONValue>());
        fieldValues.setFieldValue("age", 105);
        final Duck scrooge = new Duck(fieldValues, host);
        final boolean[] inited = new boolean[] {false};
        scrooge.on(new JSONValue(".init"), new OpRecipient() {
            @Override
            public void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
                inited[0] = true;
            }
        });

        Thread.sleep(10);
        assertTrue(inited[0]);
    }

    @Test
    public void test2n_local_ON_OFF_listeners() throws Exception {
        logger.info("2.n local listeners for on/off");
        final RememberingRecipient[] counters = new RememberingRecipient[] {
                new RememberingRecipient(),
                new RememberingRecipient(),
                new RememberingRecipient(),
                new RememberingRecipient()
        };

        final Duck duck = new Duck(host);
        Thread.sleep(10);

        duck.on(new JSONValue(".on"), counters[0]);
        duck.on(new JSONValue(".init"), counters[1]);
        duck.on(new JSONValue(".reon"), counters[2]);
        host.on(new JSONValue("/Host#gritzko.on"), counters[3]);

        List<RememberingRecipient.Triplet> oplist;
        // (0)
        /* TODO fix ???
        oplist = counters[0].getMemory();
        assertEquals("triggered by duck.on(on), duck.on(init) and host.on", 3, oplist.size());
        for (RememberingRecipient.Triplet op : oplist) {
            assertEquals(Syncable.ON, op.spec.getOp());
        } */

        // (1)
        oplist = counters[1].getMemory();
        assertEquals("triggered by duck.on(init)", 1, oplist.size());
        assertSame(duck, oplist.get(0).source);
        assertNotNull(duck.version);

        // (2)
        // doesn't get triggered if the storage is sync
        oplist = counters[2].getMemory();
        assertEquals(0, oplist.size());
        // assertEquals(Syncable.REON, spec.getOp());

        // (3)
        /* TODO fix ???
        oplist = counters[3].getMemory();
        assertEquals("triggered by host.on", 1, oplist.size());
        assertEquals(Syncable.ON, oplist.get(0).spec.getOp());
        */
    }
}
