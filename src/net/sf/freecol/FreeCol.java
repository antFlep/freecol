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

package net.sf.freecol;

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

import net.sf.freecol.start.*;
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
public final class FreeCol {

    private FreeCol() {} // Hide constructor

    /**
     * The entrypoint.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        String fcRev = ConfigPara.FREECOL_VERSION;
        ConfigPara.setFreeColRevision(fcRev);
        JarURLConnection juc;
        try {
            juc = Tools.getJarURLConnection(FreeCol.class);
        } catch (ClassCastException cce) {
            juc = null;
            System.err.println("Unable to cast class properly: "
                                       + cce.getMessage());
        } catch (IOException ioe) {
            juc = null;
            System.err.println("Unable to open class jar: "
                + ioe.getMessage());
        }
        if (juc != null) {
            try {
                String revision = Tools.readVersion(juc);
                if (revision != null) {
                    ConfigPara.setFreeColRevision(fcRev += " (Revision: " + revision + ")");
                }
            } catch (Exception e) {
                System.err.println("Unable to load Manifest: "
                    + e.getMessage());
            }
            try {
                ConfigPara.setSplashStream (Tools.getDefaultSplashStream(juc));
            } catch (Exception e) {
                System.err.println("Unable to open default splash: "
                    + e.getMessage());
            }
        }

        // We can not even emit localized error messages until we find
        // the data directory, which might have been specified on the
        // command line.
        String dataDirectoryArg = ArgsHandler.findArg("--freecol-data", args);
        String err = FreeColDirectories.setDataDirectory(dataDirectoryArg);
        if (err != null) Logging.fatal(err); // This must not fail.

        // Now we have the data directory, establish the base locale.
        // Beware, the locale may change!
        String localeArg = ArgsHandler.findArg("--default-locale", args);
        ConfigPara.setLocale(localeArg);
        Messages.loadMessageBundle(ConfigPara.getLocale());
        // Now that we can emit error messages, parse the other
        // command line arguments.
        ArgsHandler.handleArgs(args);

        // Do the potentially fatal system checks as early as possible.
        Tools.systemCheck();

        // Having parsed the command line args, we know where the user
        // directories should be, so we can set up the rest of the
        // file/directory structure.  Exit on failure here.
        StringTemplate key = FreeColDirectories.setUserDirectories();
        if (key != null) Logging.fatal(key);

        // We used to display the result of setUserDirectories when we
        // were doing a dodgy migration in 0.9 -> 0.10.  So userMsg is
        // no longer really needed, but keep it around in case something
        // similar comes up.
        String userMsg = null;

        // Now we have the log file path, start logging.
        Logging.loggingConfig(localeArg);


        // Report on where we are.
        ConfigPara.getLogger().info(Tools.getConfiguration().toString());

        // Ready to specialize into client or server.
        if (ConfigPara.isStandAloneServer()) {
            Network.startServer();
        } else {
            Network.startClient(userMsg);
        }
    }

}
