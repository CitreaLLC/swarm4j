package citrea.swarm4j.model.spec;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 21.06.2014
 *         Time: 20:48
 */
public class SpecMap {

    private Map<String, String> map = new HashMap<String, String>();

    public SpecMap(Spec vec) {
        this.add(vec);
    }

    public SpecMap(String vector) {
        this.add(new Spec(vector));
    }

    public SpecMap() {

    }

    public void add(Spec versionVector) {
        Iterator<SpecToken> it = versionVector.getTokenIterator(SpecQuant.VERSION);
        while (it.hasNext()) {
            SpecToken token = it.next();
            String time = token.getBare();
            String source = token.getExt();
            String knownTime = getKnownTime(source);
            if (time.compareTo(knownTime) > 0) {
                this.map.put(source, time);
            }
        }
    }

    public void add(String versionVector) {
        add(new Spec(versionVector));
    }

    private String getKnownTime(String source) {
        String res = this.map.get(source);
        if (res == null) { res = ""; }
        return res;
    }

    public boolean covers(SpecToken version) {
        String time = version.getBare();
        String source = version.getExt();
        String knownTime = getKnownTime(source);
        return time.compareTo(knownTime) <= 0;
    }

    public String maxTs() {
        String ts = null;
        for (Map.Entry<String, String> entry: this.map.entrySet()) {
            if (ts == null || ts.compareTo(entry.getValue()) < 0) {
                ts = entry.getValue();
            }
        }
        return ts;
    }

    public String toString(int top, String rot) {
        rot = "!" + rot;
        List<String> ret = new ArrayList<String>();
        for (Map.Entry<String, String> entry: this.map.entrySet()) {
            String ext = entry.getKey();
            String time = entry.getValue();
            ret.add("!" + time + (SpecToken.NO_AUTHOR.equals(ext) ? "" : "+" + ext));
        }
        Collections.sort(ret, new StringComparator(true));
        while (ret.size() > top || (ret.size() > 0 && ret.get(ret.size() - 1).compareTo(rot) <= 0)) {
            ret.remove(ret.size() - 1);
        }

        StringBuilder res = new StringBuilder();
        if (ret.size() > 0) {
            for (String item : ret) {
                res.append(item);
            }
        } else {
            res.append(SpecToken.ZERO_VERSION.toString());
        }
        return res.toString();
    }

    @Override
    public String toString() {
         return this.toString(10, "0");
    }

    public String get(String ext) {
        String bare = this.map.get(ext);
        if (bare == null) {
            bare = "";
        }
        return bare;
    }

    private static class StringComparator implements Comparator<String> {

        private boolean reverse;

        private StringComparator(boolean reverse) {
            this.reverse = reverse;
        }

        @Override
        public int compare(String s1, String s2) {
            return reverse ? s2.compareTo(s1) : s1.compareTo(s2);
        }
    }
}
