package moon.shared;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by Moon on 7/2/2017.
 */

public class ExceptionTools {
    public static String stackTraceToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
