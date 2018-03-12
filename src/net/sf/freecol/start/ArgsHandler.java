package net.sf.freecol.start;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.FreeColSeed;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.logging.DefaultHandler;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.option.OptionGroup;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.OSUtils;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.Controller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser;


/**
 * This class is responsible for handling the command-line arguments
 * and starting either the stand-alone server or the client-GUI.
 *
 * @see net.sf.freecol.client.FreeColClient FreeColClient
 * @see net.sf.freecol.server.FreeColServer FreeColServer
 */
public final class ArgsHandler {

    private static final Logger logger = Logger.getLogger(FreeCol.class.getName());

    /** The FreeCol release version number. */
    private static final String FREECOL_VERSION = "0.11.6";

    /** The FreeCol protocol version number. */
    private static final String FREECOL_PROTOCOL_VERSION = "0.1.6";

    /** The difficulty levels. */
    private static final String[] DIFFICULTIES = {
            "veryEasy", "easy", "medium", "hard", "veryHard"
    };

    /** The extension for FreeCol saved games. */
    public static final String  FREECOL_SAVE_EXTENSION = "fsg";

    /** The extension for FreeCol maps. */
    public static final String  FREECOL_MAP_EXTENSION = "fsm";

    /** The Java version. */
    private static final String JAVA_VERSION
            = System.getProperty("java.version");

    /** The maximum available memory. */
    private static final long MEMORY_MAX = Runtime.getRuntime().maxMemory();

    public static final String  CLIENT_THREAD = "FreeColClient:";
    public static final String  SERVER_THREAD = "FreeColServer:";
    public static final String  METASERVER_THREAD = "FreeColMetaServer:";

    /** Specific revision number (currently the git tag of trunk at release) */
    private static String       freeColRevision = null;

    /** The locale, either default or command-line specified. */
    private static Locale       locale = null;

    // Cli defaults.
    private static final Advantages ADVANTAGES_DEFAULT = Advantages.SELECTABLE;
    private static final String DIFFICULTY_DEFAULT = "model.difficulty.medium";
    private static final int    EUROPEANS_DEFAULT = 4;
    private static final int    EUROPEANS_MIN = 1;
    public static final float   GUI_SCALE_DEFAULT = 1.0f;
    private static final int    GUI_SCALE_MIN_PCT = 100;
    private static final int    GUI_SCALE_MAX_PCT = 200;
    public static final float   GUI_SCALE_MIN = GUI_SCALE_MIN_PCT / 100.0f;
    public static final float   GUI_SCALE_MAX = GUI_SCALE_MAX_PCT / 100.0f;
    private static final int    GUI_SCALE_STEP_PCT = 25;
    public static final float   GUI_SCALE_STEP = GUI_SCALE_STEP_PCT / 100.0f;
    private static final Level  LOGLEVEL_DEFAULT = Level.INFO;
    private static final String JAVA_VERSION_MIN = "1.8";
    private static final int    MEMORY_MIN = 128; // Mbytes
    private static final String META_SERVER_ADDRESS = "meta.freecol.org";
    private static final int    META_SERVER_PORT = 3540;
    private static final int    PORT_DEFAULT = 3541;
    private static final String SPLASH_DEFAULT = "splash.jpg";
    private static final String TC_DEFAULT = "freecol";
    public static final long    TIMEOUT_DEFAULT = 60L; // 1 minute
    public static final long    TIMEOUT_MIN = 10L; // 10s
    public static final long    TIMEOUT_MAX = 3600000L; // 1000hours:-)


    // Cli values.  Often set to null so the default can be applied in
    // the accessor function.
    private static boolean checkIntegrity = false;
    private static boolean consoleLogging = false;
    private static boolean debugStart = false;
    private static boolean fastStart = false;
    private static boolean headless = false;
    private static boolean introVideo = true;
    private static boolean javaCheck = true;
    private static boolean memoryCheck = true;
    private static boolean publicServer = true;
    private static boolean sound = true;
    private static boolean standAloneServer = false;

    /** The type of advantages. */
    private static Advantages advantages = null;

    /** The difficulty level id. */
    private static String difficulty = null;

    /** The number of European nations to enable by default. */
    private static int europeanCount = EUROPEANS_DEFAULT;

    /** A font override. */
    private static String fontName = null;

    /** The levels of logging in this game. */
    private static class LogLevel {

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
    private static final List<LogLevel> logLevels = new ArrayList<>();
    static {
        logLevels.add(new LogLevel("", LOGLEVEL_DEFAULT));
    }

    /** Meta-server location. */
    private static String metaServerAddress = META_SERVER_ADDRESS;
    private static int metaServerPort = META_SERVER_PORT;

    /** The client player name. */
    private static String name = null;

    /** How to name and configure the server. */
    private static int serverPort = -1;
    private static String serverName = null;

    /** A stream to get the splash image from. */
    private static InputStream splashStream;

    /** The TotalConversion / ruleset in play, defaults to "freecol". */
    private static String tc = null;

    /** The time out (seconds) for otherwise blocking commands. */
    private static long timeout = -1L;

    /**
     * The size of window to create, defaults to impossible dimensions
     * to require windowed mode with best determined screen size.
     */
    private static Dimension windowSize = new Dimension(-1, -1);

    /** How much gui elements get scaled. */
    private static float guiScale = GUI_SCALE_DEFAULT;

    /** The special client options that must be processed early. */
    private static Map<String,String> specialOptions = null;


    private ArgsHandler() {} // Hide constructor

    /**
     * Get a stream for the default splash file.
     *
     * Note: Not bothering to check for nulls as this is called in try
     * block that ignores all exceptions.
     *
     * @param juc The {@code JarURLConnection} to extract from.
     * @return A suitable {@code InputStream}, or null on error.
     * @exception IOException if the connection fails to open.
     */
    private static InputStream getDefaultSplashStream(JarURLConnection juc)
            throws IOException {
        JarFile jf = juc.getJarFile();
        ZipEntry ze = jf.getEntry(SPLASH_DEFAULT);
        return jf.getInputStream(ze);
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

    /**
     * Find an option before the real option handling can get started.
     * Takes care to use the *last* instance.
     *
     * @param option The option to find.
     * @param args The  command-line arguments.
     * @return The option's parameter.
     */
    private static String findArg(String option, String[] args) {
        for (int i = args.length - 2; i >= 0; i--) {
            if (option.equals(args[i])) {
                return args[i+1];
            }
        }
        return null;
    }

    /** Definitions for all the options. */
    private static String argDir = "cli.arg.directory";
    private static String argFile = "cli.arg.file";
    private static String[][] optionsTable = {
            // Help options
            { "?", "usage", "cli.help", null },
            { "@", "help", "cli.help", null },
            // Special early options
            { "d", "freecol-data", "cli.freecol-data", argDir },
            { "L", "default-locale", "cli.default-locale", "cli.arg.locale" },
            // Ordinary options
            { "a", "advantages", getAdvantagesDescription(), "cli.arg.advantages" },
            { null,  "check-savegame", "cli.check-savegame", argFile },
            { "O", "clientOptions", "cli.clientOptions", "cli.arg.clientOptions" },
            { "D", "debug", getDebugDescription(), "cli.arg.debug" },
            { "R", "debug-run", "cli.debug-run", "cli.arg.debugRun" },
            { "S", "debug-start", "cli.debug-start", null },
            { "D", "difficulty", "cli.difficulty", "cli.arg.difficulty" },
            { "e", "europeans", "cli.european-count", "cli.arg.europeans" },
            { null,  "fast", "cli.fast", null },
            { "f", "font", "cli.font", "cli.arg.font" },
            { "F", "full-screen", "cli.full-screen", null },
            { "g", "gui-scale", getGUIScaleDescription(), "!cli.arg.gui-scale" },
            { "H", "headless", "cli.headless", null },
            { "l", "load-savegame", "cli.load-savegame", argFile },
            { null,  "log-console", "cli.log-console", null },
            { null,  "log-file", "cli.log-file", "cli.arg.name" },
            { null,  "log-level", "cli.log-level", "cli.arg.loglevel" },
            { "m", "meta-server", "cli.meta-server", "cli.arg.metaServer" },
            { "n", "name", "cli.name", "cli.arg.name" },
            { null,  "no-intro", "cli.no-intro", null },
            { null,  "no-java-check", "cli.no-java-check", null },
            { null,  "no-memory-check", "cli.no-memory-check", null },
            { null,  "no-sound", "cli.no-sound", null },
            { null,  "no-splash", "cli.no-splash", null },
            { "p", "private", "cli.private", null },
            { "Z", "seed", "cli.seed", "cli.arg.seed" },
            { null,  "server", "cli.server", null },
            { null,  "server-name", "cli.server-name", "cli.arg.name" },
            { null,  "server-port", "cli.server-port", "cli.arg.port" },
            { "s", "splash", "cli.splash", "!" + argFile },
            { "t", "tc", "cli.tc", "cli.arg.name" },
            { "T", "timeout", "cli.timeout", "cli.arg.timeout" },
            { "C", "user-cache-directory", "cli.user-cache-directory", argDir },
            { "c", "user-config-directory", "cli.user-config-directory", argDir },
            { "u", "user-data-directory", "cli.user-data-directory", argDir },
            { "v", "version", "cli.version", null },
            { "w", "windowed", "cli.windowed", "!cli.arg.dimensions" },
    };

    /**
     * Processes the command-line arguments and takes appropriate
     * actions for each of them.
     *
     * @param args The command-line arguments.
     */
    private static void handleArgs(String[] args) {
        Options options = new Options();
        for (String[] o : optionsTable) {
            String arg = o[3];
            Option op = new Option(o[0], o[1], arg != null,
                    ((o[2].startsWith("cli.")) ? Messages.message(o[2]) : o[2]));
            if (arg != null) {
                boolean optional = false;
                if (arg.startsWith("!")) {
                    optional = true;
                    arg = arg.substring(1, arg.length());
                }
                if (arg.startsWith(argDir)
                        || arg.startsWith(argFile)) op.setType(File.class);
                if (arg.startsWith("cli.")) arg = Messages.message(arg);
                op.setArgName(arg);
                op.setOptionalArg(optional);
            }
            options.addOption(op);
        }

        CommandLineParser parser = new DefaultParser();
        boolean usageError = false;
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("help") || line.hasOption("usage")) {
                printUsage(options, 0);
            }

            if (line.hasOption("advantages")) {
                String arg = line.getOptionValue("advantages");
                Advantages a = selectAdvantages(arg);
                if (a == null) {
                    fatal(StringTemplate.template("cli.error.advantages")
                            .addName("%advantages%", getValidAdvantages())
                            .addName("%arg%", arg));
                }
            }

            if (line.hasOption("check-savegame")) {
                String arg = line.getOptionValue("check-savegame");
                if (!FreeColDirectories.setSavegameFile(arg)) {
                    fatal(StringTemplate.template("cli.err.save")
                            .addName("%string%", arg));
                }
                checkIntegrity = true;
                standAloneServer = true;
            }

            if (line.hasOption("clientOptions")) {
                String fileName = line.getOptionValue("clientOptions");
                if (!FreeColDirectories.setClientOptionsFile(fileName)) {
                    // Not fatal.
                    gripe(StringTemplate.template("cli.error.clientOptions")
                            .addName("%string%", fileName));
                }
            }

            if (line.hasOption("debug")) {
                // If the optional argument is supplied use limited mode.
                String arg = line.getOptionValue("debug");
                if (arg == null || arg.isEmpty()) {
                    // Let empty argument default to menus functionality.
                    arg = FreeColDebugger.DebugMode.MENUS.toString();
                }
                if (!FreeColDebugger.setDebugModes(arg)) { // Not fatal.
                    gripe(StringTemplate.template("cli.error.debug")
                            .addName("%modes%", FreeColDebugger.getDebugModes()));
                }
                // Keep doing this before checking log-level option!
                logLevels.add(new LogLevel("", Level.FINEST));
            }
            if (line.hasOption("debug-run")) {
                FreeColDebugger.enableDebugMode(FreeColDebugger.DebugMode.MENUS);
                FreeColDebugger.configureDebugRun(line.getOptionValue("debug-run"));
            }
            if (line.hasOption("debug-start")) {
                debugStart = true;
                FreeColDebugger.enableDebugMode(FreeColDebugger.DebugMode.MENUS);
            }

            if (line.hasOption("difficulty")) {
                String arg = line.getOptionValue("difficulty");
                String difficulty = selectDifficulty(arg);
                if (difficulty == null) {
                    fatal(StringTemplate.template("cli.error.difficulties")
                            .addName("%difficulties%", getValidDifficulties())
                            .addName("%arg%", arg));
                }
            }

            if (line.hasOption("europeans")) {
                int e = selectEuropeanCount(line.getOptionValue("europeans"));
                if (e < 0) {
                    gripe(StringTemplate.template("cli.error.europeans")
                            .addAmount("%min%", EUROPEANS_MIN));
                }
            }

            if (line.hasOption("fast")) {
                fastStart = true;
                introVideo = false;
            }

            if (line.hasOption("font")) {
                fontName = line.getOptionValue("font");
            }

            if (line.hasOption("full-screen")) {
                windowSize = null;
            }

            if (line.hasOption("gui-scale")) {
                String arg = line.getOptionValue("gui-scale");
                if(!setGUIScale(arg)) {
                    gripe(StringTemplate.template("cli.error.gui-scale")
                            .addName("%scales%", getValidGUIScales())
                            .addName("%arg%", arg));
                }
            }

            if (line.hasOption("headless")) {
                headless = true;
            }

            if (line.hasOption("load-savegame")) {
                String arg = line.getOptionValue("load-savegame");
                if (!FreeColDirectories.setSavegameFile(arg)) {
                    fatal(StringTemplate.template("cli.error.save")
                            .addName("%string%", arg));
                }
            }

            if (line.hasOption("log-console")) {
                consoleLogging = true;
            }

            if (line.hasOption("log-file")) {
                FreeColDirectories.setLogFilePath(line.getOptionValue("log-file"));
            }

            if (line.hasOption("log-level")) {
                for (String value : line.getOptionValues("log-level")) {
                    String[] s = value.split(":");
                    logLevels.add((s.length == 1)
                            ? new LogLevel("", Level.parse(s[0].toUpperCase()))
                            : new LogLevel(s[0], Level.parse(s[1].toUpperCase())));
                }
            }

            if (line.hasOption("meta-server")) {
                String arg = line.getOptionValue("meta-server");
                if (!setMetaServer(arg)) {
                    gripe(StringTemplate.template("cli.error.meta-server")
                            .addName("%arg%", arg));
                }
            }

            if (line.hasOption("name")) {
                setName(line.getOptionValue("name"));
            }

            if (line.hasOption("no-intro")) {
                introVideo = false;
            }
            if (line.hasOption("no-java-check")) {
                javaCheck = false;
            }
            if (line.hasOption("no-memory-check")) {
                memoryCheck = false;
            }
            if (line.hasOption("no-sound")) {
                sound = false;
            }
            if (line.hasOption("no-splash")) {
                splashStream = null;
            }

            if (line.hasOption("private")) {
                publicServer = false;
            }

            if (line.hasOption("server")) {
                standAloneServer = true;
            }
            if (line.hasOption("server-name")) {
                serverName = line.getOptionValue("server-name");
            }
            if (line.hasOption("server-port")) {
                String arg = line.getOptionValue("server-port");
                if (!setServerPort(arg)) {
                    fatal(StringTemplate.template("cli.error.serverPort")
                            .addName("%string%", arg));
                }
            }

            if (line.hasOption("seed")) {
                FreeColSeed.setFreeColSeed(line.getOptionValue("seed"));
            }

            if (line.hasOption("splash")) {
                String splash = line.getOptionValue("splash");
                try {
                    FileInputStream fis = new FileInputStream(splash);
                    splashStream = fis;
                } catch (FileNotFoundException fnfe) {
                    gripe(StringTemplate.template("cli.error.splash")
                            .addName("%name%", splash));
                }
            }

            if (line.hasOption("tc")) {
                setTC(line.getOptionValue("tc")); // Failure is deferred.
            }

            if (line.hasOption("timeout")) {
                String arg = line.getOptionValue("timeout");
                if (!setTimeout(arg)) { // Not fatal
                    gripe(StringTemplate.template("cli.error.timeout")
                            .addName("%string%", arg)
                            .addName("%minimum%", Long.toString(TIMEOUT_MIN)));
                }
            }

            if (line.hasOption("user-cache-directory")) {
                String arg = line.getOptionValue("user-cache-directory");
                String errMsg = FreeColDirectories.setUserCacheDirectory(arg);
                if (errMsg != null) { // Not fatal.
                    gripe(StringTemplate.template(errMsg)
                            .addName("%string%", arg));
                }
            }

            if (line.hasOption("user-config-directory")) {
                String arg = line.getOptionValue("user-config-directory");
                String errMsg = FreeColDirectories.setUserConfigDirectory(arg);
                if (errMsg != null) { // Not fatal.
                    gripe(StringTemplate.template(errMsg)
                            .addName("%string%", arg));
                }
            }

            if (line.hasOption("user-data-directory")) {
                String arg = line.getOptionValue("user-data-directory");
                String errMsg = FreeColDirectories.setUserDataDirectory(arg);
                if (errMsg != null) { // Fatal, unable to save.
                    fatal(StringTemplate.template(errMsg)
                            .addName("%string%", arg));
                }
            }

            if (line.hasOption("version")) {
                System.out.println("FreeCol " + getVersion());
                System.exit(0);
            }

            if (line.hasOption("windowed")) {
                String arg = line.getOptionValue("windowed");
                setWindowSize(arg); // Does not fail
            }

        } catch (ParseException e) {
            System.err.println("\n" + e.getMessage() + "\n");
            usageError = true;
        }
        if (usageError) printUsage(options, 1);
    }

    /**
     * Prints the usage message and exits.
     *
     * @param options The command line {@code Options}.
     * @param status The status to exit with.
     */
    private static void printUsage(Options options, int status) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -Xmx 256M -jar freecol.jar [OPTIONS]",
                options);
        System.exit(status);
    }

    /**
     * Get the specification from a given TC file.
     *
     * @param tcf The {@code FreeColTcFile} to load.
     * @param advantages An optional {@code Advantages} setting.
     * @param difficulty An optional difficulty level.
     * @return A {@code Specification}.
     */
    public static Specification loadSpecification(FreeColTcFile tcf,
                                                  Advantages advantages,
                                                  String difficulty) {
        Specification spec = null;
        try {
            if (tcf != null) spec = tcf.getSpecification();
        } catch (IOException ioe) {
            System.err.println("Spec read failed in " + tcf.getId()
                    + ": " + ioe.getMessage() + "\n");
        }
        if (spec != null) spec.prepare(advantages, difficulty);
        return spec;
    }

    /**
     * Get the specification from the specified TC.
     *
     * @return A {@code Specification}, quits on error.
     */
    private static Specification getTCSpecification() {
        Specification spec = loadSpecification(getTCFile(), getAdvantages(),
                getDifficulty());
        if (spec == null) {
            fatal(StringTemplate.template("cli.error.badTC")
                    .addName("%tc%", getTC()));
        }
        return spec;
    }

    // Accessors, mutators and support for the cli variables.

    /**
     * Gets the default advantages type.
     *
     * @return Usually Advantages.SELECTABLE, but can be overridden at the
     *     command line.
     */
    public static Advantages getAdvantages() {
        return (advantages == null) ? ADVANTAGES_DEFAULT
                : advantages;
    }

    /**
     * Sets the advantages type.
     *
     * Called from NewPanel when a selection is made.
     *
     * @param as The name of the new advantages type.
     * @return The type of advantages set, or null if none.
     */
    private static Advantages selectAdvantages(String as) {
        Advantages a = find(Advantages.values(), Messages.matchesNamed(as));
        if (a != null) setAdvantages(a);
        return a;
    }

    /**
     * Sets the advantages type.
     *
     * @param advantages The new {@code Advantages} type.
     */
    public static void setAdvantages(Advantages advantages) {
        ArgsHandler.advantages = advantages;
    }

    /**
     * Gets a comma separated list of localized advantage type names.
     *
     * @return A list of advantage types.
     */
    private static String getValidAdvantages() {
        return transform(Advantages.values(), alwaysTrue(),
                a -> Messages.getName(a), Collectors.joining(","));
    }

    /**
     * Get a description for the advantages argument.
     *
     * @return A suitable description.
     */
    private static String getAdvantagesDescription() {
        return Messages.message(StringTemplate.template("cli.advantages")
                .addName("%advantages%", getValidAdvantages()));
    }

    /**
     * Get a description for the debug argument.
     *
     * @return A suitable description.
     */
    private static String getDebugDescription() {
        return Messages.message(StringTemplate.template("cli.debug")
                .addName("%modes%", FreeColDebugger.getDebugModes()));
    }

    /**
     * Gets the difficulty level.
     *
     * @return The name of a difficulty level.
     */
    public static String getDifficulty() {
        return (difficulty == null) ? DIFFICULTY_DEFAULT : difficulty;
    }

    /**
     * Selects a difficulty level.
     *
     * @param arg The supplied difficulty argument.
     * @return The name of the selected difficulty, or null if none.
     */
    public static String selectDifficulty(String arg) {
        String difficulty
                = find(map(DIFFICULTIES, d -> "model.difficulty." + d),
                Messages.matchesName(arg));
        if (difficulty != null) setDifficulty(difficulty);
        return difficulty;
    }

    /**
     * Sets the difficulty level.
     *
     * @param difficulty The actual {@code OptionGroup}
     *     containing the difficulty level.
     */
    public static void setDifficulty(OptionGroup difficulty) {
        setDifficulty(difficulty.getId());
    }

    /**
     * Sets the difficulty level.
     *
     * @param difficulty The new difficulty.
     */
    public static void setDifficulty(String difficulty) {
        ArgsHandler.difficulty = difficulty;
    }

    /**
     * Gets the names of the valid difficulty levels.
     *
     * @return The valid difficulty levels, comma separated.
     */
    public static String getValidDifficulties() {
        return transform(DIFFICULTIES, alwaysTrue(),
                d -> Messages.getName("model.difficulty." + d),
                Collectors.joining(","));
    }

    /**
     * Get the number of European nations to enable by default.
     *
     * @return The default European nation count.
     */
    public static int getEuropeanCount() {
        return europeanCount;
    }

    /**
     * Sets the number of enabled European nations.
     *
     * @param n The number of nations to enable.
     */
    public static void setEuropeanCount(int n) {
        europeanCount = n;
    }

    /**
     * Sets the scale for GUI elements.
     *
     * @param arg The optional command line argument to be parsed.
     * @return If the argument was correctly formatted.
     */
    public static boolean setGUIScale(String arg) {
        boolean valid = true;
        if(arg == null) {
            guiScale = GUI_SCALE_MAX;
        } else {
            try {
                int n = Integer.parseInt(arg);
                if (n < GUI_SCALE_MIN_PCT) {
                    valid = false;
                    n = GUI_SCALE_MIN_PCT;
                } else if(n > GUI_SCALE_MAX_PCT) {
                    valid = false;
                    n = GUI_SCALE_MAX_PCT;
                } else if(n % GUI_SCALE_STEP_PCT != 0) {
                    valid = false;
                }
                guiScale = ((float)n / GUI_SCALE_STEP_PCT) * GUI_SCALE_STEP;
            } catch (NumberFormatException nfe) {
                valid = false;
                guiScale = GUI_SCALE_MAX;
            }
        }
        return valid;
    }

    /**
     * Gets the valid scale factors for the GUI.
     *
     * @return A string containing these.
     */
    public static String getValidGUIScales() {
        StringBuilder sb = new StringBuilder(64);
        for (int i = GUI_SCALE_MIN_PCT; i <= GUI_SCALE_MAX_PCT;
             i += GUI_SCALE_STEP_PCT) sb.append(i).append(',');
        sb.setLength(sb.length()-1);
        return sb.toString();
    }

    /**
     * Get a description of the GUI scale argument.
     *
     * @return A suitable description.
     */
    private static String getGUIScaleDescription() {
        return Messages.message(StringTemplate.template("cli.gui-scale")
                .addName("%scales%", getValidGUIScales()));
    }

    /**
     * Selects a European nation count.
     *
     * @param arg The supplied count argument.
     * @return A valid nation number, or negative on error.
     */
    public static int selectEuropeanCount(String arg) {
        try {
            int n = Integer.parseInt(arg);
            if (n >= EUROPEANS_MIN) {
                setEuropeanCount(n);
                return n;
            }
        } catch (NumberFormatException nfe) {}
        return -1;
    }

    /**
     * Get the meta-server address.
     *
     * @return The current meta-server address.
     */
    public static String getMetaServerAddress() {
        return metaServerAddress;
    }

    /**
     * Get the meta-server port.
     *
     * @return The current meta-server port.
     */
    public static int getMetaServerPort() {
        return metaServerPort;
    }

    /**
     * Set the meta-server location.
     *
     * @param arg The new meta-server location in HOST:PORT format.
     */
    private static boolean setMetaServer(String arg) {
        String[] s = arg.split(":");
        int port = -1;
        try {
            port = (s.length == 2) ? Integer.parseInt(s[1]) : -1;
        } catch (NumberFormatException nfe) {}
        if (s.length != 2 || s[0] == null || "".equals(s[0])) return false;

        metaServerAddress = s[0];
        metaServerPort = port;
        return true;
    }

    /**
     * Gets the user name.
     *
     * @return The user name, defaults to the user.name property, then to
     *     the "main.defaultPlayerName" message value.
     */
    public static String getName() {
        return (name != null) ? name
                : System.getProperty("user.name",
                Messages.message("main.defaultPlayerName"));
    }

    /**
     * Sets the user name.
     *
     * @param name The new user name.
     */
    public static void setName(String name) {
        ArgsHandler.name = name;
        logger.info("Set FreeCol.name = " + name);
    }

    /**
     * Get the selected locale.
     *
     * @return The {@code Locale} currently in use.
     */
    public static Locale getLocale() {
        return (ArgsHandler.locale == null) ? Locale.getDefault() : ArgsHandler.locale;
    }

    /**
     * Set the locale.
     *
     * @param localeArg The locale specification, null implies the
     *     default locale.
     * @return True if the {@code Locale} changed.
     */
    public static boolean setLocale(String localeArg) {
        Locale newLocale = null;
        if (localeArg == null) {
            newLocale = Locale.getDefault();
        } else {
            int index = localeArg.indexOf('.'); // Strip encoding if present
            if (index > 0) localeArg = localeArg.substring(0, index);
            newLocale = Messages.getLocale(localeArg);
        }
        if (newLocale != ArgsHandler.locale) {
            ArgsHandler.locale = newLocale;
            return true;
        }
        return false;
    }

    /**
     * Gets the current revision of game.
     *
     * @return The current version and SVN Revision of the game.
     */
    public static String getRevision() {
        return freeColRevision;
    }

    /**
     * Get the default server host name.
     *
     * @return The host name.
     */
    public static String getServerHost() {
        return InetAddress.getLoopbackAddress().getHostAddress();
    }

    /**
     * Gets the server network port.
     *
     * @return The port number.
     */
    public static int getServerPort() {
        return (serverPort < 0) ? PORT_DEFAULT : serverPort;
    }

    /**
     * Sets the server port.
     *
     * @param arg The server port number.
     * @return True if the port was set.
     */
    public static boolean setServerPort(String arg) {
        if (arg == null) return false;
        try {
            serverPort = Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Gets the current Total-Conversion.
     *
     * @return Usually TC_DEFAULT, but can be overridden at the command line.
     */
    public static String getTC() {
        return (tc == null) ? TC_DEFAULT : tc;
    }

    /**
     * Sets the Total-Conversion.
     *
     * Called from NewPanel when a selection is made.
     *
     * @param tc The name of the new total conversion.
     */
    public static void setTC(String tc) {
        ArgsHandler.tc = tc;
    }

    /**
     * Gets the FreeColTcFile for the current TC.
     *
     * @return The {@code FreeColTcFile}.
     */
    public static FreeColTcFile getTCFile() {
        return FreeColTcFile.getFreeColTcFile(getTC());
    }

    /**
     * Gets the timeout.
     * Use the command line specified one if any, otherwise default
     * to `infinite' in single player and the TIMEOUT_DEFAULT for
     * multiplayer.
     *
     * @param singlePlayer True if this is a single player game.
     * @return A suitable timeout value.
     */
    public static long getTimeout(boolean singlePlayer) {
        if (timeout < 0L) {
            timeout = (singlePlayer) ? TIMEOUT_MAX : TIMEOUT_DEFAULT;
        }
        return timeout;
    }

    /**
     * Sets the timeout.
     *
     * @param timeout A string containing the new timeout.
     * @return True if the timeout was set.
     */
    public static boolean setTimeout(String timeout) {
        try {
            long result = Long.parseLong(timeout);
            if (TIMEOUT_MIN <= result && result <= TIMEOUT_MAX) {
                ArgsHandler.timeout = result;
                return true;
            }
        } catch (NumberFormatException nfe) {}
        return false;
    }

    /**
     * Gets the current version of game.
     *
     * @return The current version of the game using the format "x.y.z",
     *         where "x" is major, "y" is minor and "z" is revision.
     */
    public static String getVersion() {
        return FREECOL_VERSION;
    }

    /**
     * Gets the current version of the FreeCol protocol.
     *
     * @return The version of the FreeCol protocol.
     */
    public static String getFreeColProtocolVersion() {
        return FREECOL_PROTOCOL_VERSION;
    }

    /**
     * Sets the window size.
     *
     * Does not fail because any empty or invalid value is interpreted as
     * `windowed but use as much screen as possible'.
     *
     * @param arg The window size specification.
     */
    private static void setWindowSize(String arg) {
        String[] xy;
        if (arg != null
                && (xy = arg.split("[^0-9]")) != null
                && xy.length == 2) {
            try {
                windowSize = new Dimension(Integer.parseInt(xy[0]),
                        Integer.parseInt(xy[1]));
            } catch (NumberFormatException nfe) {}
        }
        if (windowSize == null) windowSize = new Dimension(-1, -1);
    }


    /**
     * Generate a failure message depending on a file parameter.
     *
     * @param messageId The failure message identifier.
     * @param file The {@code File} that caused the failure.
     * @return A {@code StringTemplate} with the error message.
     */
    public static StringTemplate badFile(String messageId, File file) {
        return StringTemplate.template(messageId)
                .addName("%name%", (file == null) ? "-" : file.getPath());
    }

    /**
     * Build an error template from an exception.
     *
     * @param ex The {@code Exception} to make an error from.
     * @param fallbackKey A message key to use to make a fallback message
     *     if the exception is unsuitable.
     * @return An error {@code StringTemplate}.
     */
    public static StringTemplate errorFromException(Exception ex, String fallbackKey) {
        return errorFromException(ex, StringTemplate.template(fallbackKey));
    }

    /**
     * Build an error template from an exception.
     *
     * @param ex The {@code Exception} to make an error from.
     * @param fallback A {@code StringTemplate} to use as a fall
     *     back if the exception is unsuitable.
     * @return An error {@code StringTemplate}.
     */
    public static StringTemplate errorFromException(Exception ex, StringTemplate fallback) {
        String msg;
        return (ex == null || (msg = ex.getMessage()) == null)
                ? fallback
                : (Messages.containsKey(msg))
                ? StringTemplate.template(msg)
                : (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS))
                ? StringTemplate.name(msg)
                : fallback;
    }

    /**
     * We get a lot of lame bug reports with insufficient configuration
     * information.  Get a buffer containing as much information as we can
     * to embed in the log file and saved games.
     *
     * @return A {@code StringBuilder} full of configuration information.
     */
    public static StringBuilder getConfiguration() {
        File autosave = FreeColDirectories.getAutosaveDirectory();
        File clientOptionsFile = FreeColDirectories.getClientOptionsFile();
        File save = FreeColDirectories.getSaveDirectory();
        File userConfig = FreeColDirectories.getUserConfigDirectory();
        File userData = FreeColDirectories.getUserDataDirectory();
        File userMods = FreeColDirectories.getUserModsDirectory();
        StringBuilder sb = new StringBuilder(256);
        sb.append("Configuration:")
                .append("\n  version     ").append(getRevision())
                .append("\n  java:       ").append(JAVA_VERSION)
                .append("\n  memory:     ").append(MEMORY_MAX)
                .append("\n  locale:     ").append(getLocale())
                .append("\n  data:       ")
                .append(FreeColDirectories.getDataDirectory().getPath())
                .append("\n  userConfig: ")
                .append((userConfig == null) ? "NONE" : userConfig.getPath())
                .append("\n  userData:   ")
                .append((userData == null) ? "NONE" : userData.getPath())
                .append("\n  autosave:   ")
                .append((autosave == null) ? "NONE" : autosave.getPath())
                .append("\n  logFile:    ")
                .append(FreeColDirectories.getLogFilePath())
                .append("\n  options:    ")
                .append((clientOptionsFile == null) ? "NONE"
                        : clientOptionsFile.getPath())
                .append("\n  save:       ")
                .append((save == null) ? "NONE" : save.getPath())
                .append("\n  userMods:   ")
                .append((userMods == null) ? "NONE" : userMods.getPath())
                .append("\n  debug:      ")
                .append(FreeColDebugger.getDebugModes());
        return sb;
    }
}
