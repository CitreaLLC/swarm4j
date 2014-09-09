package citrea.swarm4j.model.oplog;

import citrea.swarm4j.model.spec.Spec;
import citrea.swarm4j.model.value.JSONValue;

import java.util.*;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 05.09.2014
 *         Time: 01:04
 */
public class ModelLogDistillator implements LogDistillator {

    /**
     * Removes redundant information from the log; as we carry a copy
     * of the log in every replica we do everythin to obtain the minimal
     * necessary subset of it.
     * As a side effect, distillLog allows up to handle some partial
     * order issues (see _ops.set).
     * @see citrea.swarm4j.model.Model#set(citrea.swarm4j.model.spec.Spec, citrea.swarm4j.model.value.JSONValue)
     * @return {*} distilled log {spec:true}
     */
    @Override
    public Map<String, JSONValue> distillLog(Map<Spec, JSONValue> oplog) {
        // explain
        Map<String, JSONValue> cumul = new HashMap<String, JSONValue>();
        Map<String, Boolean> heads = new HashMap<String, Boolean>();
        List<Spec> sets = new ArrayList<Spec>(oplog.keySet());
        Collections.sort(sets);
        Collections.reverse(sets);
        for (Spec spec : sets) {
            JSONValue val = oplog.get(spec);
            boolean notempty = false;
            Set<String> fieldsToRemove = new HashSet<String>();
            for (String field : val.getFieldNames()) {
                if (cumul.containsKey(field)) {
                    fieldsToRemove.add(field);
                } else {
                    JSONValue fieldVal = val.getFieldValue(field);
                    cumul.put(field, fieldVal);
                    notempty = !fieldVal.isEmpty(); //store last value of the field
                }
            }
            for (String field : fieldsToRemove) {
                val.removeFieldValue(field);
            }
            String source = spec.getVersion().getExt();
            if (!notempty) {
                if (heads.containsKey(source)) {
                    oplog.remove(spec);
                }
            }
            heads.put(source, true);
        }
        return cumul;
    }
}
