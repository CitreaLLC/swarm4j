package citrea.swarm4j.model;

import citrea.swarm4j.spec.Spec;
import citrea.swarm4j.spec.SpecToken;
import org.json.JSONString;

import java.util.HashMap;
import java.util.Map;

/**
 * Model object
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 26/10/13
 *         Time: 16:37
 */
public class SwarmObj {
    protected Map<String, VersionedValue> vids = new HashMap<String, VersionedValue>();

    /**
     * @return current object version vector
     */
    public Version getVersion() {
        return new Version(this);
    }

    /**
     * @param base base-version
     * @return diff between base version and current version
     */
    public SwarmObj getDiff(Version base) {
        SwarmObj res = new SwarmObj();

        for (String field : vids.keySet()) {
            final VersionedValue val = vids.get(field);
            final SpecToken version = val.getVersionAsToken();
            final String bare = version.getBare();
            final String ext = version.getExt();
            if (bare.compareTo(base.getMaxKnown(ext)) > 0) {
                res.set(field, val);
            }
        }
        return res;
    }

    /**
     * set field value
     * @param field field name
     * @param val field value
     */
    private void set(String field, VersionedValue val) {
        this.vids.put(field, val);
    }

    /**
     * get field value
     * @param field field name
     * @return current field value with version
     */
    public VersionedValue get(String field) {
        return this.vids.get(field);
    }

    /**
     * get field value (current)
     * @param field field name
     * @return current field value
     */
    public JSONString getValue(String field) {
        VersionedValue val = this.get(field);
        return val != null ? val.getValue() : null;
    }

    /**
     * object version vector
     */
    public static class Version {
        private Map<String, String> vector = new HashMap<String, String>();

        public Version(SwarmObj obj) {
            for (VersionedValue val : obj.vids.values()) {
                final SpecToken version = val.getVersionAsToken();
                final String bare = version.getBare();
                final String ext = version.getExt();
                String currentMaxVersion = vector.get(ext);
                if (currentMaxVersion == null || currentMaxVersion.compareTo(bare) <= 0) {
                    vector.put(ext, bare);
                }
            }
        }

        public String getMaxKnown(String ext) {
            return vector.get(ext);
        }
    }

    /**
     * value & it's version
     */
    public static class VersionedValue {
        private JSONString value;
        private String version;

        public VersionedValue(JSONString value, String version) {
            this.value = value;
            this.version = version;
        }

        public JSONString getValue() {
            return value;
        }

        public String getVersion() {
            return version;
        }

        public Spec getVersionAsSpec() {
            return new Spec(version);
        }

        public SpecToken getVersionAsToken() {
            return getVersionAsSpec().getVersionAsToken();
        }
    }
}
