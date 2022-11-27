package io.github.edsuns.nio.log;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 20:58
 */
@ParametersAreNonnullByDefault
public class Log {

    public static final int LEVEL_OFF = Integer.MAX_VALUE;
    public static final int LEVEL_ERROR = 1;
    public static final int LEVEL_WARN = 2;
    public static final int LEVEL_INFO = 3;
    public static final int LEVEL_DEBUG = 4;

    private static int LEVEL = LEVEL_OFF;

    private final String name;
    private final int level;

    public Log(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public void error(String format, @Nullable Object... args) {
        log(LEVEL_ERROR, format, args);
    }

    public void error(String format, Throwable throwable, @Nullable Object... args) {
        log(4, LEVEL_ERROR, format, throwable, args);
    }

    public void warn(String format, @Nullable Object... args) {
        log(LEVEL_WARN, format, args);
    }

    public void warn(String format, Throwable throwable, @Nullable Object... args) {
        log(4, LEVEL_WARN, format, throwable, args);
    }

    public void info(String format, @Nullable Object... args) {
        log(LEVEL_INFO, format, args);
    }

    public void debug(String format, @Nullable Object... args) {
        log(LEVEL_DEBUG, format, args);
    }

    public void debug(String format, Throwable throwable, @Nullable Object... args) {
        log(4, LEVEL_DEBUG, format, throwable, args);
    }

    protected void log(int level, String format, @Nullable Object... args) {
        log(5, level, format, null, args);
    }

    protected void log(int depth, int level, String format, @Nullable Throwable throwable, @Nullable Object... args) {
        if (this.level >= level) {
            String msg = "[" + level + " " + getTimestamp() + " " + name + " " + getSourceMethodName(depth) + "] " + String.format(format, args);
            System.out.println(msg);
            if (throwable != null) {
                System.err.println(print(throwable));
            }
        }
    }

    static String getTimestamp() {
        return DateTimeFormatter.ISO_INSTANT.withLocale(Locale.CHINA).format(Instant.now());
    }

    static String getSourceMethodName(int depth) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement traceElement = stackTrace[depth];
        return traceElement.getMethodName();
    }

    public static void setLevel(int level) {
        Log.LEVEL = level;
    }

    public static Log getLog(Class<?> callerClass) {
        return new Log(callerClass.getSimpleName(), LEVEL);
    }

    public static String print(Throwable throwable) {
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        throwable.printStackTrace(pw);
        pw.flush();
        return writer.toString();
    }
}
