package net.sf.freecol.start;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.logging.DefaultHandler;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.OSUtils;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Logging {

    Logging(){}

    /** The levels of logging in this game. */
    static class LogLevel {

        public final String name;
        public final Level level;
        // We need to keep a hard reference to the instantiated logger, as
        // Logger only uses weak references.
        public Logger logger;

        public LogLevel(String name, Level level) {
            this.name = name;
            this.level = level;
            this.logger = null;
        }

        public void buildLogger() {
            this.logger = Logger.getLogger("net.sf.freecol"
                    + ((this.name.isEmpty()) ? "" : "." + this.name));
            this.logger.setLevel(this.level);
        }
    }
    static final List<LogLevel> logLevels = new ArrayList<>();
    static { logLevels.add(new LogLevel("", ConfigPara.LOGLEVEL_DEFAULT)); }

    /** The special client options that must be processed early. */
    private static Map<String,String> specialOptions = null;

    public static void loggingConfig(String localeArg) {
        final Logger baseLogger = Logger.getLogger("");
        for (Handler handler : baseLogger.getHandlers()) {
            baseLogger.removeHandler(handler);
        }
        try {
            Writer writer = FreeColDirectories.getLogWriter();
            baseLogger.addHandler(new DefaultHandler(ConfigPara.consoleLogging, writer));
            for (LogLevel ll : logLevels) ll.buildLogger();
        } catch (FreeColException e) {
            System.err.println("Logging initialization failure: "
                    + e.getMessage());
            e.printStackTrace();
        }
        Thread.setDefaultUncaughtExceptionHandler((Thread thread, Throwable e) -> {
            baseLogger.log(Level.WARNING, "Uncaught exception from thread: " + thread, e);
        });

        // Now we can find the client options, allow the options
        // setting to override the locale, but only if no command line
        // option had been specified.  This works for our users who,
        // say, have machines that default to Finnish but play FreeCol in
        // English.
        //
        // If the user has selected automatic language selection, do
        // nothing, since we have already set up the default locale.
        try {
            specialOptions = ClientOptions.getSpecialOptions();
        } catch (FreeColException fce) {
            specialOptions = new HashMap<>();
            ConfigPara.getLogger().log(Level.WARNING, "Special options unavailable", fce);
        }
        String cLang;
        if (localeArg == null
                && (cLang = specialOptions.get(ClientOptions.LANGUAGE)) != null
                && !Messages.AUTOMATIC.equalsIgnoreCase(cLang)
                && ConfigPara.setLocale(cLang)) {
            Messages.loadMessageBundle(ConfigPara.getLocale());
            ConfigPara.getLogger().info("Loaded messages for " + ConfigPara.getLocale());
        }

        // Now we have the user mods directory and the locale is now
        // stable, load the TCs, the mods and their messages.
        FreeColTcFile.loadTCs();
        FreeColModFile.loadMods();
        Messages.loadModMessageBundle(ConfigPara.getLocale());

        // Handle other special options
        processSpecialOptions();
    }

    public static void processSpecialOptions() {
        LogBuilder lb = new LogBuilder(64);
        // Work around a Java 2D bug that seems to be X11 specific.
        // According to:
        //   http://www.oracle.com/technetwork/java/javase/index-142560.html
        //
        //   ``The use of pixmaps typically results in better
        //     performance. However, in certain cases, the opposite is true.''
        //
        // The standard workaround is to use -Dsun.java2d.pmoffscreen=false,
        // but this is too hard for some users, so provide an option to
        // do it easily.  However respect the initial value if present.
        //
        // Remove this if Java 2D is ever fixed.  DHYB.
        //
        final String pmoffscreen = "sun.java2d.pmoffscreen";
        final String pmoValue = System.getProperty(pmoffscreen);
        if (pmoValue == null) {
            String usePixmaps = specialOptions.get(ClientOptions.USE_PIXMAPS);
            if (usePixmaps != null) {
                System.setProperty(pmoffscreen, usePixmaps);
                lb.add(pmoffscreen, " using client option: ", usePixmaps);
            } else {
                lb.add(pmoffscreen, " unset/ignored: ");
            }
        } else {
            lb.add(pmoffscreen, " overrides client option: ", pmoValue);
        }

        // There is also this option, BR#3102.
        final String openGL = "sun.java2d.opengl";
        final String openGLValue = System.getProperty(openGL);
        if (openGLValue == null) {
            String useOpenGL = specialOptions.get(ClientOptions.USE_OPENGL);
            if (useOpenGL != null) {
                System.setProperty(openGL, useOpenGL);
                lb.add(", ", openGL, " using client option: ", useOpenGL);
            } else {
                lb.add(", ", openGL, " unset/ignored");
            }
        } else {
            lb.add(", ", openGL, " overrides client option: ", openGLValue);
        }

        // XRender is available for most unix (not MacOS?)
        xRender(lb);

        lb.log(ConfigPara.getLogger(), Level.INFO);
    }

    public static LogBuilder xRender (LogBuilder lb) {
        if (OSUtils.onUnix()) {
            final String xrender = "sun.java2d.xrender";
            final String xrValue = System.getProperty(xrender);
            if (xrValue == null) {
                String useXR = specialOptions.get(ClientOptions.USE_XRENDER);
                if (useXR != null) {
                    System.setProperty(xrender, useXR);
                    lb.add(", ", xrender, " using client option: ", useXR);
                } else {
                    lb.add(", ", xrender, " unset/ignored");
                }
            } else {
                lb.add(", ", xrender, " overrides client option: ", xrValue);
            }
        }

        return lb;
    }

    /**
     * Exit printing fatal error message.
     *
     * @param template A {@code StringTemplate} to print.
     */
    public static void fatal(StringTemplate template) {
        fatal(Messages.message(template));
    }

    /**
     * Exit printing fatal error message.
     *
     * @param err The error message to print.
     */
    public static void fatal(String err) {
        if (err == null || err.isEmpty()) {
            err = "Bogus null fatal error message";
            Thread.dumpStack();
        }
        System.err.println(err);
        System.exit(1);
    }

    /**
     * Just gripe to System.err.
     *
     * @param template A {@code StringTemplate} to print.
     */
    public static void gripe(StringTemplate template) {
        System.err.println(Messages.message(template));
    }

    /**
     * Just gripe to System.err.
     *
     * @param key A message key.
     */
    public static void gripe(String key) {
        System.err.println(Messages.message(key));
    }

    /**
     * Log a warning with a stack trace.
     *
     * @param logger The {@code Logger} to log to.
     * @param warn The warning message.
     */
    public static void trace(Logger logger, String warn) {
        FreeColDebugger.trace(logger, warn);
    }
}
