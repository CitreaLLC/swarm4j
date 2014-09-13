package citrea.swarm4j.model.clocks;

import citrea.swarm4j.model.spec.SpecToken;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 *
 * @author aleksisha
 *         Date: 09.09.2014
 *         Time: 17:15
 */
public interface Clock {

    SpecToken getLastIssuedTimestamp();

    SpecToken issueTimestamp();

    TimestampParsed parseTimestamp(SpecToken ts);

    void seeTimestamp(SpecToken ts);

    Date timestamp2date(SpecToken ts);
}
