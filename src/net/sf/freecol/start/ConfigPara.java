/**
 *  Copyright (C) 2002-2017   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.start;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.stream.Collectors;

import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.option.OptionGroup;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This class is responsible for handling the command-line arguments
 * and starting either the stand-alone server or the client-GUI.
 *
 * @see net.sf.freecol.client.FreeColClient FreeColClient
 * @see net.sf.freecol.server.FreeColServer FreeColServer
 */
public final class ConfigPara {

    private static final Logger logger = Logger.getLogger(ConfigPara.class.getName());

    /** The FreeCol release version number. */
    public static final String FREECOL_VERSION = "0.11.6";

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
    static final String JAVA_VERSION = System.getProperty("java.version");

    /** The maximum available memory. */
    static final long MEMORY_MAX = Runtime.getRuntime().maxMemory();

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
    static final int    EUROPEANS_MIN = 1;
    public static final float   GUI_SCALE_DEFAULT = 1.0f;
    private static final int    GUI_SCALE_MIN_PCT = 100;
    private static final int    GUI_SCALE_MAX_PCT = 200;
    public static final float   GUI_SCALE_MIN = GUI_SCALE_MIN_PCT / 100.0f;
    public static final float   GUI_SCALE_MAX = GUI_SCALE_MAX_PCT / 100.0f;
    private static final int    GUI_SCALE_STEP_PCT = 25;
    public static final float   GUI_SCALE_STEP = GUI_SCALE_STEP_PCT / 100.0f;
    static final Level  LOGLEVEL_DEFAULT = Level.INFO;
    static final String JAVA_VERSION_MIN = "1.8";
    static final int    MEMORY_MIN = 128; // Mbytes
    private static final String META_SERVER_ADDRESS = "meta.freecol.org";
    private static final int    META_SERVER_PORT = 3540;
    private static final int    PORT_DEFAULT = 3541;
    static final String SPLASH_DEFAULT = "splash.jpg";
    private static final String TC_DEFAULT = "freecol";
    public static final long    TIMEOUT_DEFAULT = 60L; // 1 minute
    public static final long    TIMEOUT_MIN = 10L; // 10s
    public static final long    TIMEOUT_MAX = 3600000L; // 1000hours:-)


    // Cli values.  Often set to null so the default can be applied in
    // the accessor function.
    static boolean checkIntegrity = false;
    static boolean consoleLogging = false;
    static boolean debugStart = false;
    static boolean fastStart = false;
    static boolean headless = false;
    static boolean introVideo = true;
    static boolean javaCheck = true;
    static boolean memoryCheck = true;
    static boolean publicServer = true;
    static boolean sound = true;
    static boolean standAloneServer = false;

    /** The type of advantages. */
    private static Advantages advantages = null;

    /** The difficulty level id. */
    private static String difficulty = null;

    /** The number of European nations to enable by default. */
    private static int europeanCount = EUROPEANS_DEFAULT;

    /** A font override. */
    static String fontName = null;

    /** Meta-server location. */
    private static String metaServerAddress = META_SERVER_ADDRESS;
    private static int metaServerPort = META_SERVER_PORT;

    /** The client player name. */
    private static String name = null;

    /** How to name and configure the server. */
    static int serverPort = -1;
    static String serverName = null;

    /** A stream to get the splash image from. */
    static InputStream splashStream;

    /** The TotalConversion / ruleset in play, defaults to "freecol". */
    private static String tc = null;

    /** The time out (seconds) for otherwise blocking commands. */
    private static long timeout = -1L;

    /**
     * The size of window to create, defaults to impossible dimensions
     * to require windowed mode with best determined screen size.
     */
    static Dimension windowSize = new Dimension(-1, -1);

    /** How much gui elements get scaled. */
    static float guiScale = GUI_SCALE_DEFAULT;

    /** The special client options that must be processed early. */
    private static Map<String,String> specialOptions = null;

    /**
     * Get the specification from a given TC file.
     *
     * @param tcf The {@code FreeColTcFile} to load.
     * @param advantages An optional {@code Advantages} setting.
     * @param difficulty An optional difficulty level.
     * @return A {@code Specification}.
     */
    public static Specification loadSpecification(FreeColTcFile tcf, Advantages advantages, String difficulty) {
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
    static Specification getTCSpecification() {
        Specification spec = loadSpecification(getTCFile(), getAdvantages(),
                getDifficulty());
        if (spec == null) {
            Logging.fatal(StringTemplate.template("cli.error.badTC")
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
    static Advantages selectAdvantages(String as) {
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
        ConfigPara.advantages = advantages;
    }

    /**
     * Gets a comma separated list of localized advantage type names.
     *
     * @return A list of advantage types.
     */
    static String getValidAdvantages() {
        return transform(Advantages.values(), alwaysTrue(),
                a -> Messages.getName(a), Collectors.joining(","));
    }

    /**
     * Get a description for the advantages argument.
     *
     * @return A suitable description.
     */
    static String getAdvantagesDescription() {
        return Messages.message(StringTemplate.template("cli.advantages")
                .addName("%advantages%", getValidAdvantages()));
    }

    /**
     * Get a description for the debug argument.
     *
     * @return A suitable description.
     */
    static String getDebugDescription() {
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
        ConfigPara.difficulty = difficulty;
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
    static String getGUIScaleDescription() {
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
    static boolean setMetaServer(String arg) {
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
        ConfigPara.name = name;
        logger.info("Set FreeCol.name = " + name);
    }

    /**
     * Get the selected locale.
     *
     * @return The {@code Locale} currently in use.
     */
    public static Locale getLocale() {
        return (ConfigPara.locale == null) ? Locale.getDefault() : ConfigPara.locale;
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
        if (newLocale != ConfigPara.locale) {
            ConfigPara.locale = newLocale;
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
        ConfigPara.tc = tc;
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
                ConfigPara.timeout = result;
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
    static void setWindowSize(String arg) {
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

    public static void setFreeColRevision(String freeColRevision) {
        ConfigPara.freeColRevision = freeColRevision;
    }

    public static boolean isStandAloneServer() {
        return standAloneServer;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setSplashStream(InputStream splashStream) {
        ConfigPara.splashStream = splashStream;
    }
}
