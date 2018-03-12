package net.sf.freecol.start;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.JarURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColSeed;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.option.OptionGroup;
import static net.sf.freecol.common.util.CollectionUtils.*;

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
            { "a", "advantages", ConfigPara.getAdvantagesDescription(), "cli.arg.advantages" },
            { null,  "check-savegame", "cli.check-savegame", argFile },
            { "O", "clientOptions", "cli.clientOptions", "cli.arg.clientOptions" },
            { "D", "debug", ConfigPara.getDebugDescription(), "cli.arg.debug" },
            { "R", "debug-run", "cli.debug-run", "cli.arg.debugRun" },
            { "S", "debug-start", "cli.debug-start", null },
            { "D", "difficulty", "cli.difficulty", "cli.arg.difficulty" },
            { "e", "europeans", "cli.european-count", "cli.arg.europeans" },
            { null,  "fast", "cli.fast", null },
            { "f", "font", "cli.font", "cli.arg.font" },
            { "F", "full-screen", "cli.full-screen", null },
            { "g", "gui-scale", ConfigPara.getGUIScaleDescription(), "!cli.arg.gui-scale" },
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
                Advantages a = ConfigPara.selectAdvantages(arg);
                if (a == null) {
                    Logging.fatal(StringTemplate.template("cli.error.advantages")
                            .addName("%advantages%", ConfigPara.getValidAdvantages())
                            .addName("%arg%", arg));
                }
            }

            if (line.hasOption("check-savegame")) {
                String arg = line.getOptionValue("check-savegame");
                if (!FreeColDirectories.setSavegameFile(arg)) {
                    Logging.fatal(StringTemplate.template("cli.err.save")
                            .addName("%string%", arg));
                }
                ConfigPara.checkIntegrity = true;
                ConfigPara.standAloneServer = true;
            }

            if (line.hasOption("clientOptions")) {
                String fileName = line.getOptionValue("clientOptions");
                if (!FreeColDirectories.setClientOptionsFile(fileName)) {
                    // Not fatal.
                    Logging.gripe(StringTemplate.template("cli.error.clientOptions").addName("%string%", fileName));
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
                    Logging.gripe(StringTemplate.template("cli.error.debug")
                            .addName("%modes%", FreeColDebugger.getDebugModes()));
                }
                // Keep doing this before checking log-level option!
                ConfigPara.logLevels.add(new ConfigPara.LogLevel("", Level.FINEST));
            }
            if (line.hasOption("debug-run")) {
                FreeColDebugger.enableDebugMode(FreeColDebugger.DebugMode.MENUS);
                FreeColDebugger.configureDebugRun(line.getOptionValue("debug-run"));
            }
            if (line.hasOption("debug-start")) {
                ConfigPara.debugStart = true;
                FreeColDebugger.enableDebugMode(FreeColDebugger.DebugMode.MENUS);
            }

            if (line.hasOption("difficulty")) {
                String arg = line.getOptionValue("difficulty");
                String difficulty = ConfigPara.selectDifficulty(arg);
                if (difficulty == null) {
                    Logging.fatal(StringTemplate.template("cli.error.difficulties")
                            .addName("%difficulties%", ConfigPara.getValidDifficulties())
                            .addName("%arg%", arg));
                }
            }

            if (line.hasOption("europeans")) {
                int e = ConfigPara.selectEuropeanCount(line.getOptionValue("europeans"));
                if (e < 0) {
                    Logging.gripe(StringTemplate.template("cli.error.europeans")
                            .addAmount("%min%", ConfigPara.EUROPEANS_MIN));
                }
            }

            if (line.hasOption("fast")) {
                ConfigPara.fastStart = true;
                ConfigPara.introVideo = false;
            }

            if (line.hasOption("font")) {
                ConfigPara.fontName = line.getOptionValue("font");
            }

            if (line.hasOption("full-screen")) {
                ConfigPara.windowSize = null;
            }

            if (line.hasOption("gui-scale")) {
                String arg = line.getOptionValue("gui-scale");
                if(!ConfigPara.setGUIScale(arg)) {
                    Logging.gripe(StringTemplate.template("cli.error.gui-scale")
                            .addName("%scales%", ConfigPara.getValidGUIScales())
                            .addName("%arg%", arg));
                }
            }

            if (line.hasOption("headless")) {
                ConfigPara.headless = true;
            }

            if (line.hasOption("load-savegame")) {
                String arg = line.getOptionValue("load-savegame");
                if (!FreeColDirectories.setSavegameFile(arg)) {
                    Logging.fatal(StringTemplate.template("cli.error.save")
                            .addName("%string%", arg));
                }
            }

            if (line.hasOption("log-console")) {
                ConfigPara.consoleLogging = true;
            }

            if (line.hasOption("log-file")) {
                FreeColDirectories.setLogFilePath(line.getOptionValue("log-file"));
            }

            if (line.hasOption("log-level")) {
                for (String value : line.getOptionValues("log-level")) {
                    String[] s = value.split(":");
                    ConfigPara.logLevels.add((s.length == 1)
                            ? new ConfigPara.LogLevel("", Level.parse(s[0].toUpperCase()))
                            : new ConfigPara.LogLevel(s[0], Level.parse(s[1].toUpperCase())));
                }
            }

            if (line.hasOption("meta-server")) {
                String arg = line.getOptionValue("meta-server");
                if (!ConfigPara.setMetaServer(arg)) {
                    Logging.gripe(StringTemplate.template("cli.error.meta-server").addName("%arg%", arg));
                }
            }

            if (line.hasOption("name")) {
                ConfigPara.setName(line.getOptionValue("name"));
            }

            if (line.hasOption("no-intro")) {
                ConfigPara.introVideo = false;
            }
            if (line.hasOption("no-java-check")) {
                ConfigPara.javaCheck = false;
            }
            if (line.hasOption("no-memory-check")) {
                ConfigPara.memoryCheck = false;
            }
            if (line.hasOption("no-sound")) {
                ConfigPara.sound = false;
            }
            if (line.hasOption("no-splash")) {
                ConfigPara.splashStream = null;
            }

            if (line.hasOption("private")) {
                ConfigPara.publicServer = false;
            }

            if (line.hasOption("server")) {
                ConfigPara.standAloneServer = true;
            }
            if (line.hasOption("server-name")) {
                ConfigPara.serverName = line.getOptionValue("server-name");
            }
            if (line.hasOption("server-port")) {
                String arg = line.getOptionValue("server-port");
                if (!ConfigPara.setServerPort(arg)) {
                    Logging.fatal(StringTemplate.template("cli.error.serverPort").addName("%string%", arg));
                }
            }

            if (line.hasOption("seed")) {
                FreeColSeed.setFreeColSeed(line.getOptionValue("seed"));
            }

            if (line.hasOption("splash")) {
                String splash = line.getOptionValue("splash");
                try {
                    FileInputStream fis = new FileInputStream(splash);
                    ConfigPara.splashStream = fis;
                } catch (FileNotFoundException fnfe) {
                    Logging.gripe(StringTemplate.template("cli.error.splash").addName("%name%", splash));
                }
            }

            if (line.hasOption("tc")) {
                ConfigPara.setTC(line.getOptionValue("tc")); // Failure is deferred.
            }

            if (line.hasOption("timeout")) {
                String arg = line.getOptionValue("timeout");
                if (!ConfigPara.setTimeout(arg)) { // Not fatal
                    Logging.gripe(StringTemplate.template("cli.error.timeout")
                            .addName("%string%", arg)
                            .addName("%minimum%", Long.toString(ConfigPara.TIMEOUT_MIN)));
                }
            }

            if (line.hasOption("user-cache-directory")) {
                String arg = line.getOptionValue("user-cache-directory");
                String errMsg = FreeColDirectories.setUserCacheDirectory(arg);
                if (errMsg != null) { // Not fatal.
                    Logging.gripe(StringTemplate.template(errMsg).addName("%string%", arg));
                }
            }

            if (line.hasOption("user-config-directory")) {
                String arg = line.getOptionValue("user-config-directory");
                String errMsg = FreeColDirectories.setUserConfigDirectory(arg);
                if (errMsg != null) { // Not fatal.
                    Logging.gripe(StringTemplate.template(errMsg).addName("%string%", arg));
                }
            }

            if (line.hasOption("user-data-directory")) {
                String arg = line.getOptionValue("user-data-directory");
                String errMsg = FreeColDirectories.setUserDataDirectory(arg);
                if (errMsg != null) { // Fatal, unable to save.
                    Logging.fatal(StringTemplate.template(errMsg).addName("%string%", arg));
                }
            }

            if (line.hasOption("version")) {
                System.out.println("FreeCol " + ConfigPara.getVersion());
                System.exit(0);
            }

            if (line.hasOption("windowed")) {
                String arg = line.getOptionValue("windowed");
                ConfigPara.setWindowSize(arg); // Does not fail
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
}
