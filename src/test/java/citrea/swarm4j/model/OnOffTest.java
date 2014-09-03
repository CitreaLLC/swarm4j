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
    public static final SpecToken UPLINK = new SpecToken("#swarm~up");
    public static final SpecToken DOWNLINK = new SpecToken("#client");

    private Thread uplinkStorageThread;
    private Thread uplinkThread;
    private Thread downlinkThread;
    private Thread downlinkStorageThread;

    private XInMemoryStorage uplinkStorage;
    private XInMemoryStorage downlinkStorage;

    private Host uplink;
    private Host downlink;

    private LoopbackConnection up_down_link;

    @Before
    public void setUp() throws Exception {

        uplinkStorage = new XInMemoryStorage(new SpecToken("#dummy"));
        uplinkStorageThread = new Thread(uplinkStorage);
        uplinkStorageThread.start();

        uplink = new Host(UPLINK, uplinkStorage);
        uplink.registerType(Duck.class);
        uplink.registerType(Thermometer.class);
        uplinkThread = new Thread(uplink);
        uplinkThread.start();
        while (!uplink.ready()) {
            Thread.sleep(10);
        }


        downlinkStorage = new XInMemoryStorage(new SpecToken("#cache"));
        downlinkStorageThread = new Thread(downlinkStorage);
        downlinkStorageThread.start();

        downlink = new Host(DOWNLINK, downlinkStorage);
        downlink.registerType(Duck.class);
        downlink.registerType(Thermometer.class);
        downlinkThread = new Thread(downlink);
        downlinkThread.start();
        while (!downlink.ready()) {
            Thread.sleep(10);
        }

        up_down_link = new LoopbackConnection();
        uplink.accept(up_down_link);
        downlink.connect(up_down_link.getPaired());
    }

    @After
    public void tearDown() throws Exception {
        downlink.close();
        downlink = null;
        downlinkThread.interrupt();
        downlinkThread = null;

        uplink.close();
        uplink = null;
        uplinkThread.interrupt();
        uplinkThread = null;

        uplinkStorage = null;
        uplinkStorageThread.interrupt();
        uplinkStorageThread = null;

        downlinkStorage = null;
        downlinkStorageThread.interrupt();
        downlinkStorageThread = null;
    }

    @Test
    public void test3a_serialized_on_reon() throws SwarmException, InterruptedException {
        logger.info("3.a serialized on, reon");
        // that's the default uplink.getSources = function () {return [storage]};
        final Spec THERM_ID = new Spec("/Thermometer#room");

        downlink.on(new JSONValue(THERM_ID.addToken(Syncable.INIT).toString()), new OpRecipient() {
            @Override
            public void deliver(Spec spec, JSONValue value, OpRecipient source) throws SwarmException {
                Thermometer obj = (Thermometer) downlink.objects.get(THERM_ID);
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

        Thermometer o = (Thermometer) uplink.objects.get(THERM_ID);
        assertNotNull(o);
        assertEquals(22, o.t);
    }

    @Test
    @Ignore
    public void test3b_pipe_reconnect_backoff() throws SwarmException, InterruptedException {
        logger.info("3.b pipe reconnect, backoff");
        Thermometer thermometer = uplink.get(Thermometer.class);

        // OK. The idea is to connect/disconnect it 100 times then
        // check that the state is OK, there are no zombie listeners
        // no objects/hosts, log is 1 record long (distilled) etc

        downlink.on(JSONValue.convert(thermometer.getTypeId().addToken(Model.SET).toString()), new OpRecipient() {
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
            equal(spec.id(), 'downlink~C1');
            setTimeout(function(){ //:)
                downlink1.disconnect('uplink~C');
            }, 100);
        });
        */
    }
}
