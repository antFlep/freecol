package net.sf.freecol.start;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.Controller;

import java.io.File;
import java.util.logging.Level;

public class Network {

    // The major final actions.

    /**
     * Start a client.
     *
     * @param userMsg An optional user message key.
     */
    public static void startClient(String userMsg) {
        Specification spec = null;
        File savegame = FreeColDirectories.getSavegameFile();
        if (Config.debugStart) {
            spec = Config.getTCSpecification();
        } else if (Config.fastStart) {
            if (savegame == null) {
                // continue last saved game if possible,
                // otherwise start a new one
                savegame = FreeColDirectories.getLastSaveGameFile();
                if (savegame == null) {
                    spec = Config.getTCSpecification();
                }
            }
            // savegame was specified on command line
        }

        // Sort out the special graphics options before touching the GUI
        // (which is initialized by FreeColClient).  These options control
        // graphics pipeline initialization, and are ineffective if twiddled
        // after the first Java2D call is made.


        final FreeColClient freeColClient = new FreeColClient(Config.splashStream, Config.fontName, Config.guiScale, Config.headless);
        freeColClient.startClient(Config.windowSize, userMsg, Config.sound, Config.introVideo, savegame, spec);
    }

    /**
     * Start the server.
     */
    public static void startServer() {
        Config.logger.info("Starting stand-alone server.");
        FreeColServer freeColServer;
        File saveGame = FreeColDirectories.getSavegameFile();
        if (saveGame != null) {
            try {
                final FreeColSavegameFile fis
                        = new FreeColSavegameFile(saveGame);
                freeColServer = new FreeColServer(fis, (Specification)null,
                        Config.serverPort, Config.serverName);
            } catch (Exception e) {
                Config.logger.log(Level.SEVERE, "Load fail", e);
                Config.fatal(Messages.message(Config.badFile("error.couldNotLoad", saveGame))
                        + ": " + e);
                freeColServer = null;
            }

            integrityCheck(freeColServer);

            if (freeColServer == null) return;
        } else {
            Specification spec = Config.getTCSpecification();
            try {
                freeColServer = new FreeColServer(Config.publicServer, false, spec,
                        Config.serverPort, Config.serverName);
            } catch (Exception e) {
                Config.fatal(Messages.message("server.initialize")
                        + ": " + e.getMessage());
                return;
            }
            if (Config.publicServer && freeColServer != null
                    && !freeColServer.getPublicServer()) {
                Config.gripe(Messages.message("server.noRouteToServer"));
            }
        }

        String quit = FreeCol.SERVER_THREAD + "Quit Game";
        final Controller controller = freeColServer.getController();
        Runtime.getRuntime().addShutdownHook(new Thread(quit) {
            @Override
            public void run() {
                controller.shutdown();
            }
        });
    }

    public static void integrityCheck(FreeColServer freeColServer) {
        if (Config.checkIntegrity) {
            String k;
            int ret;
            int check;

            check = (freeColServer == null) ? -1 : freeColServer.getIntegrity();

            switch (check) {
                case 1:
                    k = "cli.check-savegame.success";
                    ret = 0;
                    break;
                case 0:
                    k = "cli.check-savegame.fixed";
                    ret = 2;
                    break;
                case -1: default:
                    k = "cli.check-savegame.failed";
                    ret = 3;
                    break;
            }
            if (freeColServer == null) {
                Config.logger.warning("Integrity test blocked");
            }
            Config.gripe(StringTemplate.template(k)
                    .add("%log%", FreeColDirectories.getLogFilePath()));
            System.exit(ret);
        }
    }
}
