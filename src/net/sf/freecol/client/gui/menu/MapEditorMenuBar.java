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

package net.sf.freecol.client.gui.menu;

import java.awt.event.KeyEvent;
import java.awt.event.MouseMotionListener;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.ChangeWindowedModeAction;
import net.sf.freecol.client.gui.action.DetermineHighSeasAction;
import net.sf.freecol.client.gui.action.DisplayGridAction;
import net.sf.freecol.client.gui.action.DisplayTileTextAction;
import net.sf.freecol.client.gui.action.DisplayTileTextAction.DisplayText;
import net.sf.freecol.client.gui.action.MapControlsAction;
import net.sf.freecol.client.gui.action.NewAction;
import net.sf.freecol.client.gui.action.NewEmptyMapAction;
import net.sf.freecol.client.gui.action.OpenAction;
import net.sf.freecol.client.gui.action.PreferencesAction;
import net.sf.freecol.client.gui.action.QuitAction;
import net.sf.freecol.client.gui.action.SaveAction;
import net.sf.freecol.client.gui.action.ScaleMapAction;
import net.sf.freecol.client.gui.action.ShowMainAction;
import net.sf.freecol.client.gui.action.StartMapAction;
import net.sf.freecol.client.gui.action.ZoomInAction;
import net.sf.freecol.client.gui.action.ZoomOutAction;
import net.sf.freecol.client.gui.panel.Utility;


/**
 * The menu bar used when running in editor mode.
 *
 * <br><br>
 *
 * The menu bar that is displayed on the top left corner of the
 * {@code Canvas}.
 *
 * @see InGameMenuBar
 */
public class MapEditorMenuBar extends FreeColMenuBar {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorMenuBar.class.getName());


    /**
     * Creates a new {@code MapEditorMenuBar}. This menu bar will include
     * all of the submenus and items.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param listener An optional mouse motion listener.
     */
    public MapEditorMenuBar(final FreeColClient freeColClient, MouseMotionListener listener) {
        super(freeColClient);

        // Add a mouse listener so that autoscrolling can happen in this menubar
        this.addMouseMotionListener(listener);
        reset();
    }


    /**
     * Resets this menu bar.
     */
    @Override
    public final void reset() {
        removeAll();

        buildGameMenu();
        buildViewMenu();
        buildToolsMenu();
        buildColopediaMenu();

        update();
    }

    private void buildGameMenu() {
        // --> Game
        JMenu menu = Utility.localizedMenu("menuBar.game");
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_G);

        menu.add(getMenuItem(NewAction.ID));
        menu.add(getMenuItem(NewEmptyMapAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(OpenAction.ID));
        menu.add(getMenuItem(SaveAction.ID));
        menu.add(getMenuItem(StartMapAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(PreferencesAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(ShowMainAction.ID));
        menu.add(getMenuItem(QuitAction.ID));

        add(menu);
    }

    private void buildViewMenu() {
        // --> View
        JMenu menu = Utility.localizedMenu("menuBar.view");
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_V);

        menu.add(getCheckBoxMenuItem(MapControlsAction.ID));
        menu.add(getCheckBoxMenuItem(DisplayGridAction.ID));
        menu.add(getCheckBoxMenuItem(ChangeWindowedModeAction.ID));

        menu.addSeparator();
        ButtonGroup tileTextGroup = new ButtonGroup();
        for (DisplayText type : DisplayText.values()) {
            menu.add(getRadioButtonMenuItem(DisplayTileTextAction.ID + type.getKey(),
                                            tileTextGroup));
        }

        menu.addSeparator();
        menu.add(getMenuItem(ZoomInAction.ID));
        menu.add(getMenuItem(ZoomOutAction.ID));

        add(menu);
    }

    private void buildToolsMenu() {
        // --> Tools
        JMenu menu = Utility.localizedMenu("menuBar.tools");
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_T);

        menu.add(getMenuItem(ScaleMapAction.ID));
        menu.add(getMenuItem(DetermineHighSeasAction.ID));

        add(menu);
    }

}
