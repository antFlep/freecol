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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;


/**
 * This class is responsible for handling the command-line arguments
 * and starting either the stand-alone server or the client-GUI.
 *
 * @see net.sf.freecol.client.FreeColClient FreeColClient
 * @see net.sf.freecol.server.FreeColServer FreeColServer
 */
public final class Tools {

    public static void systemCheck() {
        if (Parameters.javaCheck && Parameters.JAVA_VERSION_MIN.compareTo(Parameters.JAVA_VERSION) > 0) {
            Logging.fatal(StringTemplate.template("main.javaVersion")
                    .addName("%version%", Parameters.JAVA_VERSION)
                    .addName("%minVersion%", Parameters.JAVA_VERSION_MIN));
        }
        if (Parameters.memoryCheck && Parameters.MEMORY_MAX < Parameters.MEMORY_MIN * 1000000) {
            Logging.fatal(StringTemplate.template("main.memory")
                    .addAmount("%memory%", Parameters.MEMORY_MAX)
                    .addAmount("%minMemory%", Parameters.MEMORY_MIN));
        }
    }

    /**
     * Get the JarURLConnection from a class.
     *
     * @param c The {@code Class} to get the connection for.
     * @return The {@code JarURLConnection}.
     * @exception IOException if the connection fails to open.
     */
    public static JarURLConnection getJarURLConnection(Class c) throws IOException, ClassCastException {
        String resourceName = "/" + c.getName().replace('.', '/') + ".class";
        URL url = c.getResource(resourceName);
        return (JarURLConnection)url.openConnection();
    }

    /**
     * Extract the package version from the class.
     *
     * @param juc The {@code JarURLConnection} to extract from.
     * @return A value of the package version attribute.
     * @exception IOException if the manifest is not available.
     */
    public static String readVersion(JarURLConnection juc) throws IOException {
        Manifest mf = juc.getManifest();
        return (mf == null) ? null
                : mf.getMainAttributes().getValue("Package-Version");
    }

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
    public static InputStream getDefaultSplashStream(JarURLConnection juc) throws IOException {
        JarFile jf = juc.getJarFile();
        ZipEntry ze = jf.getEntry(Parameters.SPLASH_DEFAULT);
        return jf.getInputStream(ze);
    }

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
                .append("\n  version     ").append(Parameters.getRevision())
                .append("\n  java:       ").append(Parameters.JAVA_VERSION)
                .append("\n  memory:     ").append(Parameters.MEMORY_MAX)
                .append("\n  locale:     ").append(Parameters.getLocale())
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
