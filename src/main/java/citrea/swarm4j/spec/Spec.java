package citrea.swarm4j.spec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specifier "/type#id.member!version"
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 *         Date: 26/10/13
 *         Time: 15:02
 */
public class Spec {

    public static final String RS_Q_TOK_EXT = ("([$])(=(?:\\+=)?)")
            .replaceAll("$", SpecQuant.allCodes)
            .replaceAll("=", SpecToken.RS_TOK);

    public static final long EPOCH = 1262275200000L; // 1 Jan 2010 (milliseconds)

    public static final String BASE64 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~";

    private String type;
    private String id;
    private String member;
    private String version;

    public Spec (Spec copy) {
        this.type = copy.type;
        this.id = copy.id;
        this.member = copy.member;
        this.version = copy.version;
    }

    public Spec (String specAsString) {
        this.type = null;
        this.id = null;
        this.member = null;
        this.version = null;
        if (specAsString == null) {
            return;
        }

        Pattern pattern = Pattern.compile(RS_Q_TOK_EXT);
        Matcher matcher = pattern.matcher(specAsString);
        while (matcher.find()) {
            SpecQuant quant = SpecQuant.forCode(matcher.group(1));
            String token = matcher.group(2);
            switch (quant) {
                case TYPE:
                    this.type = token;
                    break;
                case MEMBER:
                    this.member = token;
                    break;
                case ID:
                    this.id = token;
                    break;
                case VERSION:
                    this.version = token;
                    break;
                default:
                    //TODO skip or throw Exception ?
            }
        }
    }

    @Override
    public String toString() {
        return (this.type != null ? SpecQuant.TYPE.toString() + this.type : "") +
                (this.member != null ? SpecQuant.MEMBER.toString() + this.member : "") +
                (this.id != null ? SpecQuant.ID.toString() + this.id : "") +
                (this.version != null ? SpecQuant.VERSION.toString() + this.version : "");
    }

    //TODO what about making Spec unmutable? then build new scoped Spec here
    public void setScope(Spec scope) {
        if (scope.type != null) {
            this.type = scope.type;
        }
        if (scope.member != null) {
            this.member = scope.member;
        }
        if (scope.id != null) {
            this.id = scope.id;
        }
        if (scope.version != null) {
            this.version = scope.version;
        }
    }

    //TODO can be cached if Spec is unmutable
    public boolean isEmpty() {
        return this.type == null &&
                this.member == null &&
                this.id == null &&
                this.version == null;
    }


    public Spec getParent() {
        Spec ret = new Spec(this);
        if (ret.version != null) {
            ret.version = null;
        } else if (ret.member != null) {
            ret.member = null;
        } else if (ret.id != null) {
            ret.id = null;
        } else if (ret.type != null) {
            ret.type = null;
        }
        return ret;
    }

    public Spec child(String id) {
        Spec child = new Spec(this);
        if (child.type != null) {
            child.type = id;
        } else if (child.id != null) {
            child.id = id;
        } else if (child.member != null) {
            child.member = id;
        } else if (child.version != null) {
            child.version = id;
        } //TODO else throw exception or return clone?
        return child;
    }

    public String getVersion() {
        return version;
    }

    public SpecToken getVersionAsToken() {
        return new SpecToken(version);
    }

    /* TODO ???
    // 3-parameter signature
    //  * specifier (or a base64 string)
    //  * value anything but a function
    //  * source/callback - anything that can receive events
    public static void normalizeSig3(String host, Spec spec, Object value, Object source) {
        int len = args.length;
        if (len==0 || len>3) throw new IllegalArgumentException("invalid number of arguments");
        if (typeof(args[len-1])=="function") {
            args[len-1] = { set:args[len-1] }; /// BAD
        }
        if (len<3 && args[len-1] && typeof(args[len-1].set)==='function') {
            args[2] = args[len-1];
            args[len-1] = null;
        }
        if (!args[1] && typeof(args[0])==='object' &&
                args[0].constructor!==Spec && args[0].constructor!==String) {
            args[1] = args[0];
            args[0] = null;
        }
        if (!args[0] || args[0].constructor!==Spec) {
            if (args[0] && args[0].toString().replace(Spec.reQTokExt,'')==='') {
                args[0] = new Spec(args[0].toString());
            } else {
                var spec = new Spec(host.scope());
                if (args[0])
                    spec[host._childKey] = args[0].toString();
                args[0] = spec;
            }
        }
    }
    */

}
