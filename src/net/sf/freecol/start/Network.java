package net.sf.freecol.start;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.Controller;

import java.io.File;
import java.io.IOException;
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
        if (ConfigPara.debugStart) {
            spec = ConfigPara.getTCSpecification();
        } else if (ConfigPara.fastStart) {
            if (savegame == null) {
                // continue last saved game if possible,
                // otherwise start a new one
                savegame = FreeColDirectories.getLastSaveGameFile();
                if (savegame == null) {
                    spec = ConfigPara.getTCSpecification();
                }
            }
            // savegame was specified on command line
        }

        // Sort out the special graphics options before touching the GUI
        // (which is initialized by FreeColClient).  These options control
        // graphics pipeline initialization, and are ineffective if twiddled
        // after the first Java2D call is made.


        final FreeColClient freeColClient = new FreeColClient(ConfigPara.splashStream,
                ConfigPara.fontName,
                ConfigPara.guiScale,
                ConfigPara.headless);
        freeColClient.startClient(ConfigPara.windowSize,
                userMsg,
                ConfigPara.sound,
                ConfigPara.introVideo,
                savegame,
                spec);
    }

    /**
     * Start the server.
     */
    public static void startServer() {
        Logging.logger.info("Starting stand-alone server.");
        FreeColServer freeColServer;
        File saveGame = FreeColDirectories.getSavegameFile();
        if (saveGame != null) {
            try {
                final FreeColSavegameFile fis
                        = new FreeColSavegameFile(saveGame);
                freeColServer = new FreeColServer(fis, (Specification)null,
                        ConfigPara.serverPort, ConfigPara.serverName);
            } catch (Exception e) {
                Logging.logger.log(Level.SEVERE, "Load fail", e);
                Logging.fatal(Messages.message(Tools.badFile("error.couldNotLoad", saveGame))
                        + ": " + e);
                freeColServer = null;
            }

            integrityCheck(freeColServer);

            if (freeColServer == null) return;
        } else {
            Specification spec = ConfigPara.getTCSpecification();
            try {
                freeColServer = new FreeColServer(ConfigPara.publicServer, false, spec,
                        ConfigPara.serverPort, ConfigPara.serverName);
            } catch (Exception e) {
                Logging.fatal(Messages.message("server.initialize")
                        + ": " + e.getMessage());
                return;
            }
            if (ConfigPara.publicServer && freeColServer != null
                    && !freeColServer.getPublicServer()) {
                Logging.gripe(Messages.message("server.noRouteToServer"));
            }
        }

        String quit = ConfigPara.SERVER_THREAD + "Quit Game";
        final Controller controller = freeColServer.getController();
        Runtime.getRuntime().addShutdownHook(new Thread(quit) {
            @Override
            public void run() {
                controller.shutdown();
            }
        });
    }

    public static void integrityCheck(FreeColServer freeColServer) {
        if (ConfigPara.checkIntegrity) {
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
                Logging.logger.warning("Integrity test blocked");
            }
            Logging.gripe(StringTemplate.template(k)
                    .add("%log%", FreeColDirectories.getLogFilePath()));
            System.exit(ret);
        }
    }
}
