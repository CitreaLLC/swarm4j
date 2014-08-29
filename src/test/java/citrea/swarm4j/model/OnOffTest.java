package citrea.swarm4j.model;

import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;
import citrea.swarm4j.storage.Storage;
import com.sun.corba.se.impl.orbutil.concurrent.Sync;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
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
public class OnOffTest {

    public static final Logger logger = LoggerFactory.getLogger(OnOffTest.class);
    public static final SpecToken UPLINK = new SpecToken("#swarm~up");
    public static final SpecToken DOWNLINK = new SpecToken("#swarm~down");

    private XInMemoryStorage storage;
    private Host uplink;
    private Host downlink;

    @Before
    public void prepareStoragesAndHosts() throws SwarmException {
        storage = new XInMemoryStorage();
        uplink = new Host(UPLINK, storage);
        uplink.registerType(Thermometer.class);

        downlink = new Host(DOWNLINK, null);
        downlink.registerType(Thermometer.class);
    }

    //TODO @Test
    public void test3a_serialized_on_reon() throws SwarmException {
        logger.info("3.a serialized on, reon");
        // that's the default uplink.getSources = function () {return [storage]};
        final Spec THERM_ID = new Spec("/Thermometer#room");

        //TODO link uplink with downlink
        //uplink.accept("loopback:3a");
        //downlink.connect("loopback:a3"); // TODO possible mismatch

        //downlink.getSources = function () {return [lowerPipe]};

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

        //TODO async... (250ms timeout)
        Thermometer o = (Thermometer) uplink.objects.get(THERM_ID);
        assertNotNull(o);
        assertEquals(22, o.t);
        //downlink.disconnect(lowerPipe);
        downlink.disconnect();
    }

    //TODO @Test
    public void test3b_pipe_reconnect_backoff() {
        logger.info("3.b pipe reconnect, backoff");
        /*
        var storage = new Storage(false);
        var uplink = new Host('swarm~3b', 0, storage);
        var downlink = new Host('client~3b');

        uplink.accept('loopback:3b');
        downlink.connect('loopback:b3'); // TODO possible mismatch

        var thermometer = uplink.get(Thermometer), i=0;

        // OK. The idea is to connect/disconnect it 100 times then
        // check that the state is OK, there are no zombie listeners
        // no objects/hosts, log is 1 record long (distilled) etc

        var ih = setInterval(function(){
            thermometer.set({t:i});
            if (i++==30) {
                ok(thermometer._lstn.length<=3); // storage and maybe the client
                clearInterval(ih);
                start();
                uplink.disconnect();
            }
        },100);

        // FIXME sets are NOT aggregated; make a test for that

        downlink.on(thermometer.spec().toString() + '.set', function i(spec, val, obj){
            if (spec.op()==='set') {
                var loopbackPipes = env.streams.loopback.pipes;
                var stream = loopbackPipes['b3'];
                stream && stream.close();
            }
        });
        */
    }

    //TODO @Test
    public void test3c_disconnection_events() throws SwarmException {
        logger.info("3.c Disconnection events");

        //TODO bind uplink with downlink

        /*
        uplink.accept('loopback:3c');
        downlink1.connect('loopback:c3');

        env.localhost = downlink1;

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
