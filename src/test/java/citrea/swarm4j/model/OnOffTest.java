package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.pipe.LoopbackConnection;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;
import org.json.JSONException;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 28.08.2014
 *         Time: 22:48
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OnOffTest {

    public static final Logger logger = LoggerFactory.getLogger(OnOffTest.class);
    public static final SpecToken SERVER = new SpecToken("#swarm~up");
    public static final SpecToken CLIENT = new SpecToken("#client");

    private Thread dummyStorageThread;
    private Thread serverThread;
    private Thread downlinkThread;
    //TODO cache-storage private Thread cacheStorageThread;

    private XInMemoryStorage dummyStorage;
    //TODO cache-storage private XInMemoryStorage cacheStorage;

    private Host server;
    private Host client;

    private LoopbackConnection up_down_link;

    @Before
    public void setUp() throws Exception {

        dummyStorage = new XInMemoryStorage(new SpecToken("#dummy"));
        dummyStorageThread = new Thread(dummyStorage);
        dummyStorageThread.start();

        server = new Host(SERVER, dummyStorage);
        server.registerType(Duck.class);
        server.registerType(Thermometer.class);
        serverThread = new Thread(server);
        serverThread.start();
        while (!server.ready()) {
            Thread.sleep(10);
        }


        //cacheStorage = new XInMemoryStorage(new SpecToken("#cache"));
        //cacheStorageThread = new Thread(cacheStorage);
        //cacheStorageThread.start();

        client = new Host(CLIENT);
        client.registerType(Duck.class);
        client.registerType(Thermometer.class);
        downlinkThread = new Thread(client);
        downlinkThread.start();
        while (!client.ready()) {
            Thread.sleep(10);
        }

        up_down_link = new LoopbackConnection();
        server.accept(up_down_link);
        client.connect(up_down_link.getPaired());
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        client = null;
        downlinkThread.interrupt();
        downlinkThread = null;

        server.close();
        server = null;
        serverThread.interrupt();
        serverThread = null;

        dummyStorage = null;
        dummyStorageThread.interrupt();
        dummyStorageThread = null;
    }

    @Test
    public void test3a_serialized_on_reon() throws SwarmException, InterruptedException {
        logger.info("3.a serialized on, reon");
        // that's the default server.getSources = function () {return [storage]};
        final Spec THERM_ID = new Spec("/Thermometer#room");

        client.on(new JSONValue(THERM_ID.addToken(Syncable.INIT).toString()), new OpRecipient() {
            @Override
            public void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
                Thermometer obj = (Thermometer) client.objects.get(THERM_ID);
                JSONValue fieldValues = new JSONValue(new HashMap<String, JSONValue>());
                try {
                    fieldValues.setFieldValue("t", 22);
                } catch (JSONException e) {
                    throw new SwarmException(e.getMessage(), e);
                }
                obj.set(fieldValues);
            }
        });
        Thread.sleep(100);

        Thermometer o = (Thermometer) server.objects.get(THERM_ID);
        assertNotNull(o);
        assertEquals(22, o.t);
    }

    @Test
    @Ignore
    public void test3b_pipe_reconnect_backoff() throws SwarmException, InterruptedException {
        logger.info("3.b pipe reconnect, backoff");
        Thermometer thermometer = server.get(Thermometer.class);

        // OK. The idea is to connect/disconnect it 100 times then
        // check that the state is OK, there are no zombie listeners
        // no objects/hosts, log is 1 record long (distilled) etc

        client.on(JSONValue.convert(thermometer.getTypeId().addToken(Model.SET).toString()), new OpRecipient() {
            @Override
            public void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
                if (Model.SET.equals(spec.getOp())) {
                    /* TODO pipe reconnect
                    var loopbackPipes = env.streams.loopback.pipes;
                    var stream = loopbackPipes['b3'];
                    stream && stream.close();
                    */
                }

            }
        });

        for (int i = 0; i < 30; i++) {
            HashMap<String, JSONValue> fieldValues = new HashMap<String, JSONValue>();
            fieldValues.put("t", JSONValue.convert(i));
            thermometer.set(JSONValue.convert(fieldValues));
            Thread.sleep(100);
        }

        // FIXME sets are NOT aggregated; make a test for that
    }

    @Test
    @Ignore
    public void test3c_disconnection_events() throws SwarmException {
        logger.info("3.c Disconnection events");

        /*
        expect(3);

        downlink1.on('.reoff', function (spec,val,src) {
            equal(src, downlink1);
            ok(!src.isUplinked());
            start();
        });

        downlink1.on('.reon', function (spec,val,src) {
            equal(spec.id(), 'client~C1');
            setTimeout(function(){ //:)
                downlink1.disconnect('server~C');
            }, 100);
        });
        */
    }
}
