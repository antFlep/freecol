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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.AssignTradeRouteAction;
import net.sf.freecol.client.gui.action.BuildColonyAction;
import net.sf.freecol.client.gui.action.CenterAction;
import net.sf.freecol.client.gui.action.ChangeAction;
import net.sf.freecol.client.gui.action.ChangeWindowedModeAction;
import net.sf.freecol.client.gui.action.ChatAction;
import net.sf.freecol.client.gui.action.ClearOrdersAction;
import net.sf.freecol.client.gui.action.DeclareIndependenceAction;
import net.sf.freecol.client.gui.action.DisbandUnitAction;
import net.sf.freecol.client.gui.action.DisplayBordersAction;
import net.sf.freecol.client.gui.action.DisplayGridAction;
import net.sf.freecol.client.gui.action.DisplayTileTextAction;
import net.sf.freecol.client.gui.action.DisplayTileTextAction.DisplayText;
import net.sf.freecol.client.gui.action.EndTurnAction;
import net.sf.freecol.client.gui.action.EuropeAction;
import net.sf.freecol.client.gui.action.ExecuteGotoOrdersAction;
import net.sf.freecol.client.gui.action.FindSettlementAction;
import net.sf.freecol.client.gui.action.FortifyAction;
import net.sf.freecol.client.gui.action.GotoAction;
import net.sf.freecol.client.gui.action.GotoTileAction;
import net.sf.freecol.client.gui.action.LoadAction;
import net.sf.freecol.client.gui.action.MapControlsAction;
import net.sf.freecol.client.gui.action.NewAction;
import net.sf.freecol.client.gui.action.OpenAction;
import net.sf.freecol.client.gui.action.PreferencesAction;
import net.sf.freecol.client.gui.action.QuitAction;
import net.sf.freecol.client.gui.action.ReconnectAction;
import net.sf.freecol.client.gui.action.RenameAction;
import net.sf.freecol.client.gui.action.ReportCargoAction;
import net.sf.freecol.client.gui.action.ReportColonyAction;
import net.sf.freecol.client.gui.action.ReportContinentalCongressAction;
import net.sf.freecol.client.gui.action.ReportEducationAction;
import net.sf.freecol.client.gui.action.ReportExplorationAction;
import net.sf.freecol.client.gui.action.ReportForeignAction;
import net.sf.freecol.client.gui.action.ReportHighScoresAction;
import net.sf.freecol.client.gui.action.ReportHistoryAction;
import net.sf.freecol.client.gui.action.ReportIndianAction;
import net.sf.freecol.client.gui.action.ReportLabourAction;
import net.sf.freecol.client.gui.action.ReportMilitaryAction;
import net.sf.freecol.client.gui.action.ReportNavalAction;
import net.sf.freecol.client.gui.action.ReportProductionAction;
import net.sf.freecol.client.gui.action.ReportReligionAction;
import net.sf.freecol.client.gui.action.ReportRequirementsAction;
import net.sf.freecol.client.gui.action.ReportTradeAction;
import net.sf.freecol.client.gui.action.ReportTurnAction;
import net.sf.freecol.client.gui.action.RetireAction;
import net.sf.freecol.client.gui.action.SaveAction;
import net.sf.freecol.client.gui.action.SaveAndQuitAction;
import net.sf.freecol.client.gui.action.SentryAction;
import net.sf.freecol.client.gui.action.ShowDifficultyAction;
import net.sf.freecol.client.gui.action.ShowGameOptionsAction;
import net.sf.freecol.client.gui.action.ShowMainAction;
import net.sf.freecol.client.gui.action.ShowMapGeneratorOptionsAction;
import net.sf.freecol.client.gui.action.SkipUnitAction;
import net.sf.freecol.client.gui.action.TilePopupAction;
import net.sf.freecol.client.gui.action.ToggleViewModeAction;
import net.sf.freecol.client.gui.action.TradeRouteAction;
import net.sf.freecol.client.gui.action.UnloadAction;
import net.sf.freecol.client.gui.action.WaitAction;
import net.sf.freecol.client.gui.action.ZoomInAction;
import net.sf.freecol.client.gui.action.ZoomOutAction;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TileImprovementType;


/**
 * This is the menu bar used in-game.
 *
 * <br><br>
 *
 * The menu bar that is displayed on the top left corner of the
 * {@code Canvas}.
 *
 * @see MapEditorMenuBar
 */
public class InGameMenuBar extends FreeColMenuBar {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(InGameMenuBar.class.getName());
    

    /**
     * Creates a new {@code FreeColMenuBar}. This menu bar will include
     * all of the submenus and items.
     *
     * @param freeColClient The main controller.
     * @param listener An optional mouse motion listener.
     */
    public InGameMenuBar(FreeColClient freeColClient, MouseMotionListener listener) {
        // FIXME: FreeColClient should not have to be passed in to
        // this class.  This is only a menu bar, it doesn't need a
        // reference to the main controller.  The only reason it has
        // one now is because DebugMenu needs it.  And DebugMenu needs
        // it because it is using inner classes for ActionListeners
        // and those inner classes use the reference.  If those inner
        // classes were in seperate classes, when they were created,
        // they could use the FreeColClient reference of the
        // ActionManger.  So DebugMenu needs to be refactored to remove
        // inner classes so that this MenuBar can lose its unnecessary
        // reference to the main controller.  See FreeColMenuTest.
        //
        // Okay, I lied.. the update() and paintComponent() methods in
        // this MenuBar use freeColClient, too. But so what.  Move
        // those to another class too. :)
        super(freeColClient);

        // Add a mouse listener so that autoscrolling can happen in
        // this menubar
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
        buildOrdersMenu();
        buildReportMenu();
        buildColopediaMenu();

        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
            add(new DebugMenu(freeColClient));
        }

        update();
    }

    private void buildGameMenu() {
        // --> Game
        JMenu menu = Utility.localizedMenu("menuBar.game");
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_G);

        menu.add(getMenuItem(NewAction.ID));
        menu.add(getMenuItem(OpenAction.ID));
        menu.add(getMenuItem(SaveAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(PreferencesAction.ID));
        menu.add(getMenuItem(ReconnectAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(ChatAction.ID));
        menu.add(getMenuItem(DeclareIndependenceAction.ID));
        menu.add(getMenuItem(EndTurnAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(ShowMainAction.ID));
        menu.add(getMenuItem(ReportHighScoresAction.ID));
        menu.add(getMenuItem(RetireAction.ID));
        menu.add(getMenuItem(SaveAndQuitAction.ID));
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
        menu.add(getCheckBoxMenuItem(DisplayBordersAction.ID));
        menu.add(getMenuItem(ToggleViewModeAction.id));
        menu.add(getCheckBoxMenuItem(ChangeWindowedModeAction.ID));

        menu.addSeparator();
        ButtonGroup group = new ButtonGroup();
        for (DisplayText type : DisplayText.values()) {
            menu.add(getRadioButtonMenuItem(DisplayTileTextAction.ID
                    + type.getKey(), group));
        }

        menu.addSeparator();
        menu.add(getMenuItem(ZoomInAction.id));
        menu.add(getMenuItem(ZoomOutAction.id));
        menu.add(getMenuItem(CenterAction.ID));
        menu.add(getMenuItem(TilePopupAction.id));

        menu.addSeparator();

        menu.add(getMenuItem(EuropeAction.ID));
        menu.add(getMenuItem(TradeRouteAction.id));
        menu.add(getMenuItem(FindSettlementAction.ID));

        add(menu);
    }

    private void buildOrdersMenu() {
        // --> Orders
        JMenu menu = Utility.localizedMenu("menuBar.orders");
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_O);

        menu.add(getMenuItem(SentryAction.ID));
        menu.add(getMenuItem(FortifyAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(GotoAction.ID));
        menu.add(getMenuItem(GotoTileAction.ID));
        menu.add(getMenuItem(ExecuteGotoOrdersAction.ID));
        menu.add(getMenuItem(AssignTradeRouteAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(BuildColonyAction.ID));
        // Insert all Improvements here:
        for (TileImprovementType type : freeColClient.getGame().getSpecification()
                 .getTileImprovementTypeList()) {
            if (!type.isNatural()) {
                menu.add(getMenuItem(type.getSuffix() + "Action"));
            }
        }
        menu.addSeparator();

        menu.add(getMenuItem(LoadAction.ID));
        menu.add(getMenuItem(UnloadAction.id));

        menu.addSeparator();

        menu.add(getMenuItem(WaitAction.id));
        menu.add(getMenuItem(SkipUnitAction.ID));
        menu.add(getMenuItem(ChangeAction.ID));

        menu.addSeparator();

        menu.add(getMenuItem(ClearOrdersAction.ID));
        menu.add(getMenuItem(RenameAction.ID));
        menu.add(getMenuItem(DisbandUnitAction.ID));

        add(menu);
    }

    private void buildReportMenu() {
        // --> Report

        JMenu menu = Utility.localizedMenu("menuBar.report");
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_R);

        menu.add(getMenuItem(ReportReligionAction.ID));
        menu.add(getMenuItem(ReportLabourAction.ID));
        menu.add(getMenuItem(ReportColonyAction.ID));
        menu.add(getMenuItem(ReportForeignAction.ID));
        menu.add(getMenuItem(ReportIndianAction.ID));
        menu.add(getMenuItem(ReportContinentalCongressAction.ID));
        menu.add(getMenuItem(ReportMilitaryAction.ID));
        menu.add(getMenuItem(ReportNavalAction.ID));
        menu.add(getMenuItem(ReportTradeAction.ID));
        menu.add(getMenuItem(ReportTurnAction.ID));
        menu.add(getMenuItem(ReportRequirementsAction.ID));
        menu.add(getMenuItem(ReportCargoAction.ID));
        menu.add(getMenuItem(ReportExplorationAction.ID));
        menu.add(getMenuItem(ReportHistoryAction.ID));
        menu.add(getMenuItem(ReportProductionAction.ID));
        menu.add(getMenuItem(ReportEducationAction.ID));
        menu.add(getMenuItem(ShowDifficultyAction.ID));
        menu.add(getMenuItem(ShowGameOptionsAction.ID));
        menu.add(getMenuItem(ShowMapGeneratorOptionsAction.ID));

        add(menu);

    }


    /**
     * Paints information about gold, tax and year.
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (freeColClient != null && freeColClient.getMyPlayer() != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                                 RenderingHints.VALUE_RENDER_QUALITY);
            final int gold = freeColClient.getMyPlayer().getGold();
            String displayString =
                Messages.message(StringTemplate.template("menuBar.statusLine")
                    .addAmount("%gold%", gold)
                    .addAmount("%tax%", freeColClient.getMyPlayer().getTax())
                    .addAmount("%score%",
                        freeColClient.getMyPlayer().getScore())
                    .addStringTemplate("%year%",
                        freeColClient.getGame().getTurn().getLabel()));
            Rectangle2D displayStringBounds
                = g2d.getFontMetrics().getStringBounds(displayString, g);
            int y = Math.round(12f*freeColClient.getGUI().getImageLibrary().getScaleFactor())
                  + 3 + getInsets().top;
            g2d.drawString(displayString, getWidth() - 10 - (int) displayStringBounds.getWidth(), y);
        }
    }

}
