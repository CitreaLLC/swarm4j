package citrea.swarm4j.storage;

import citrea.swarm4j.model.Host;
import citrea.swarm4j.model.SwarmException;
import citrea.swarm4j.model.Syncable;
import citrea.swarm4j.model.callback.OpRecipient;
import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.spec.SpecQuant;
import citrea.swarm4j.model.spec.SpecToken;
import citrea.swarm4j.model.value.JSONValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An improvised filesystem-based storage implementation.
 * Objects are saved into separate files in a hashed directory
 * tree. Ongoing operations are streamed into a log file.
 * One can go surprisingly far with this kind of an approach.
 * https://news.ycombinator.com/item?id=7872239
 *
 * v load:   preload existing log chunks
 *   on:     load state, add tail, send, reon base=????
 *   patch:  state=> save state; log=> append
 *   op:     append
 *   unload: may flush all states
 * v onflush:new flush
 *
 * Difference: with/without direct access to the state.
 * Will not request state from the client side anyway.
 *
 * @author aleksisha
 *         Date: 25.08.2014
 *         Time: 00:45
 */
public class FileStorage extends Storage {

    public static final Logger logger = LoggerFactory.getLogger(FileStorage.class);

    public static final int MAX_LOG_SIZE = 1 << 15;

    private Map<Spec, Map<Spec, JSONValue>> tails = new HashMap<Spec, Map<Spec, JSONValue>>();
    private String dir;
    private int logCount;
    private Queue<String> dirtyQueue = new ArrayDeque<String>();
    private FileChannel logFile = null;
    private int logSize = 0;
    private long pulling;


    public FileStorage(SpecToken id, String dir) throws SwarmException {
        super(id);
        this.host = null; //will be set during Host creation

        this.dir = dir;
        File storageRoot = new File(dir);
        if (!storageRoot.exists()) {
            if (!storageRoot.mkdir()) {
                throw new SwarmException("Can't create directory: " + storageRoot.getName());
            }
        }
        File storageLogs = new File(dir + "/_log");
        if (!storageLogs.exists()) {
            if (!storageLogs.mkdir()) {
                throw new SwarmException("Can't create logs directory: " + storageLogs.getName());
            }
        }
        this.id = new SpecToken("#file"); //path.basename(dir);

        //for time() method
        this.lastTs = "";
        this.tsSeq = 0;

        this.logCount = 0;
        this.loadTails();
        this.rotateLog();
    }

    @Override
    protected void appendToLog(Spec ti, JSONValue verop2val) throws SwarmException {
        Map<Spec, JSONValue> tail = this.tails.get(ti);
        if (tail == null) {
            tail = new HashMap<Spec, JSONValue>();
            this.tails.put(ti, tail);
        }
        // stash ops in RAM (can't seek in the log file so need that)
        for (String verop : verop2val.getFieldNames()) {
            tail.put(new Spec(verop), verop2val.getFieldValue(verop));
        }
        // queue the object for state flush
        this.dirtyQueue.add(ti.toString());
        // serialize the op as JSON
        Map<String, JSONValue> o = new HashMap<String, JSONValue>();
        o.put(ti.toString(), verop2val);  // TODO annoying
        String buf = new JSONValue(o).toJSONString() + ",\n";
        ByteBuffer bbuf = ByteBuffer.wrap(buf.getBytes());
        // append JSON to the log file
        try {
            while (bbuf.hasRemaining()) {
                this.logFile.write(bbuf);
            }
        } catch (IOException e) {
            throw new SwarmException("Error writing operation to log: " + ti.toString(), e);
        }
        this.logSize += bbuf.capacity();
        if (this.logSize > MAX_LOG_SIZE) {
            this.rotateLog();
        }
        // We flush objects to files one at a time to keep HDD seek rates
        // at reasonable levels; if something fails we don't get stuck for
        // more than 1 second.
        if (this.pulling == 0 || this.pulling < new Date().getTime() - 1000) {
            this.pullState(ti);
        }
    }

    public void pullState(Spec ti) throws SwarmException {
        String spec;
        while ((spec = this.dirtyQueue.poll()) != null) {
            if (spec.matches("\\d+")) {
                // TODO ??? String cleared = this.logFileName(Integer.valueOf(spec));
                // FIXME we should not delete the file before the state will be flushed to the disk
            } else if (this.tails.containsKey(new Spec(spec))) {
                break; // flush it
            }
        }
        if (spec == null) {
            // all states flushed
            return;
        }
        this.pulling = new Date().getTime();
        // Request the host to send us the full state patch.
        // Only a live object can integrate log tail into the state,
        // so we use this trick. As object lifecycles differ in Host
        // and FileStorage we can't safely access the object directly.
        final Spec onSpec = ti.addToken(new SpecToken(SpecQuant.VERSION, this.time())).addToken(Syncable.ON);
        this.host.deliver(onSpec, new JSONValue(".init!0"), this);
    }

    @Override
    public void patch(Spec spec, JSONValue patch) throws SwarmException {
        Spec ti = spec.getTypeId();
        if (patch.getFieldValue("_version").isEmpty()) { // no full state, just the tail
            this.appendToLog(ti, patch.getFieldValue("_tail"));
            return;
        }
        // in the [>on <patch1 <reon >patch2] handshake pattern, we
        // are currently at the patch2 stage, so the state in this
        // patch also includes the tail which was sent in patch1
        this.tails.remove(ti);

        String stateFileName = this.stateFileName(ti);
        int pos = stateFileName.lastIndexOf("/");
        String dir = stateFileName.substring(0, pos);
        File folder = new File(dir);
        // I believe FAT is cached (no disk seek) so existsSync()
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                throw new SwarmException("Error creating state-folder: " + dir);
            }
        }
        // finally, send JSON to the file
        try {
            RandomAccessFile stateFile = new RandomAccessFile(stateFileName, "w");
            try {
                String json = patch.toJSONString();
                ByteBuffer buf = ByteBuffer.wrap(json.getBytes(Charset.forName("UTF-8")));

                FileChannel channel = stateFile.getChannel();
                channel.position(0);
                while (buf.hasRemaining()) {
                    channel.write(buf);
                }
                channel.force(false);
                channel.truncate(buf.capacity());
            } finally {
                stateFile.close();
            }

            this.pulling = 0;
            this.pullState(ti); // may request next object

        } catch (IOException e) {
            throw new SwarmException(e.getMessage(), e);
        }
    }

    public String logFileName(int count) {
        return this.dir + "/_log/log" + SpecToken.int2base(count, 8);
    }

    public int parseLogFileName(String fileName) {
        Pattern p = Pattern.compile("/.*?(\\w{8})$/");
        Matcher m = p.matcher(fileName);
        if (!m.matches()) {
            throw new IllegalArgumentException("Wrong log file name: " + fileName);
        }
        return SpecToken.base2int(m.group(1));
    }

    public String stateFileName(Spec spec) {
        StringBuilder base = new StringBuilder();
        base.append(this.dir);
        base.append("/");
        base.append(spec.getType().getBody());
        base.append("/");
        base.append(spec.getId().getBody());
        return base.toString(); // TODO hashing (issue: may break FAT caching?)
    }

    // Once the current log file exceeds some size, we start a new one.
    // Once all ops are saved in object-state files, a log file is rm'ed.
    public void rotateLog() throws SwarmException {
        try {
            if (this.logFile != null) {
                this.logFile.force(false);
                this.logFile.close();
                this.dirtyQueue.add(String.valueOf(this.logCount));
            }
            RandomAccessFile fw = new RandomAccessFile(this.logFileName(++this.logCount), "a");
            this.logFile = fw.getChannel();
            this.logSize = 0;
        } catch (IOException e) {
            throw new SwarmException("Error rotating log: " + e.getMessage(), e);
        }
    }

    @Override
    public void on(Spec spec, JSONValue base, OpRecipient replica) throws SwarmException {
        Spec ti = spec.getTypeId();
        String stateFileName = this.stateFileName(ti);

        // read in the state
        FileInputStream stateFileStream;
        try {
            stateFileStream = new FileInputStream(stateFileName);
        } catch (FileNotFoundException e) {
            stateFileStream = null;
        }

        // load state
        JSONValue state = new JSONValue(Collections.<String, JSONValue>emptyMap());
        if (stateFileStream == null) {
            state.setFieldValue("_version", new JSONValue("!0"));
        } else {
            try {
                JSONTokener jsonTokener = new JSONTokener(stateFileStream);
                Object jsonObject = jsonTokener.nextValue();

                if (!(jsonObject instanceof JSONObject)) {
                    logger.warn("onMessage message must be a JSON");
                    throw new JSONException("Waiting object, got " + (jsonObject == null ? "null" : jsonObject.getClass()));
                }

                state = JSONValue.convert(jsonObject);
                Map<Spec, JSONValue> tail = this.tails.get(ti);
                if (tail != null) {
                    JSONValue state_tail = state.getFieldValue("_tail");
                    if (state_tail.isEmpty()) {
                        state_tail = new JSONValue(new HashMap<String, JSONValue>());
                    }
                    for (Map.Entry<Spec, JSONValue> op : tail.entrySet()) {
                        state_tail.setFieldValue(op.getKey().toString(), op.getValue());
                    }
                    state.setFieldValue("_tail", state_tail);
                }


            } catch (JSONException e) {
                throw new SwarmException("Wrong state file format: " + e.getMessage(), e);
            } finally {
                try {
                    stateFileStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        Spec tiv = ti.addToken(spec.getVersion());
        replica.deliver(tiv.addToken(Syncable.PATCH), state, this);
        String versionVector = Storage.stateVersionVector(state);
        replica.deliver(tiv.addToken(Syncable.REON), new JSONValue(versionVector), this);
    }

    @Override
    public void off(Spec spec, OpRecipient src) throws SwarmException {
        // if (this.tails[ti]) TODO half-close
        src.deliver(spec.overrideToken(Syncable.REON), JSONValue.NULL, this);
    }

    /**
     * Load all existing log files on startup.
     * Object-state files will be read on demand but we can't seek inside
     * log files so load 'em as this.tails
     */
    public void loadTails() throws SwarmException {
        String path = this.dir + "/_log";
        File logsFolder = new File(path);
        if (!logsFolder.isDirectory()) {
            throw new IllegalStateException("Path is not a directory: " + logsFolder.getAbsolutePath());
        }
        File[] logFiles = logsFolder.listFiles();
        if (logFiles == null) {
            return;
        }

        ByteBuffer buf = ByteBuffer.allocate(4096);
        try {
            for (File logFile : logFiles) {
                int count = this.parseLogFileName(logFile.getName());
                this.logCount = Math.max(count, this.logCount);

                StringBuilder json = new StringBuilder();
                json.append("[");

                CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
                RandomAccessFile file = new RandomAccessFile(logFile, "r");
                try {
                    FileChannel channel = file.getChannel();
                    buf.clear();
                    while (channel.read(buf) > 0) {
                        buf.flip();
                        json.append(decoder.reset().decode(buf));
                        buf.clear();
                    }
                } finally {
                    file.close();
                }

                json.append("{}]");

                JSONTokener jsonTokener = new JSONTokener(json.toString());
                Object jsonObject = jsonTokener.nextValue();

                if (!(jsonObject instanceof JSONArray)) {
                    logger.warn("data must be a JSON array");
                    throw new JSONException("Waiting array, got " + (jsonObject == null ? "null" : jsonObject.getClass()));
                }

                JSONArray arr = (JSONArray) jsonObject;
                for (int i = 0, l = arr.length(); i < l; i++) {
                    JSONValue block = JSONValue.convert(arr.get(i));
                    for (String tidoid : block.getFieldNames()) {
                        Map<Spec, JSONValue> tail = this.tails.get(new Spec(tidoid));
                        JSONValue ops = block.getFieldValue(tidoid);
                        if (tail == null) {
                            tail = new HashMap<Spec, JSONValue>();
                            this.tails.put(new Spec(tidoid), tail);
                            this.dirtyQueue.add(tidoid);
                        }
                        for (String vidop : ops.getFieldNames()) {
                            tail.put(new Spec(vidop), ops.getFieldValue(vidop));
                        }
                    }
                    this.dirtyQueue.add(String.valueOf(this.logCount));
                }
            }
        } catch (IOException e) {
            throw new SwarmException("Error loading log: " + e.getMessage(), e);
        } catch (JSONException e) {
            throw new SwarmException("Error loading log: " + e.getMessage(), e);
        }
    }

    public Spec getTypeId() {
        return new Spec(Host.HOST, id);
    }

}
