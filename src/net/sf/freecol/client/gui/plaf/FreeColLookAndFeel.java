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

package net.sf.freecol.client.gui.plaf;

import java.awt.Font;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.client.gui.ImageLibrary;


/**
 * Implements the FreeCol look and feel.
 */
public class FreeColLookAndFeel extends MetalLookAndFeel {

    private static final Logger logger = Logger.getLogger(FreeColLookAndFeel.class.getName());

    private static final String BRIGHT_PANEL_UI
        = "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI";
    private static final String TRANSPARENT_PANEL_UI
        = "net.sf.freecol.client.gui.plaf.FreeColTransparentPanelUI";

    private static final Class uiClasses[] = {
        FreeColButtonUI.class,
        FreeColCheckBoxUI.class,
        FreeColComboBoxUI.class,
        FreeColFileChooserUI.class,
        FreeColLabelUI.class,
        FreeColListUI.class,
        FreeColMenuBarUI.class,
        FreeColMenuItemUI.class,
        FreeColOptionPaneUI.class,
        FreeColPanelUI.class,
        FreeColPopupMenuUI.class,
        FreeColRadioButtonUI.class,
        FreeColScrollPaneUI.class,
        FreeColTableHeaderUI.class,
        FreeColTableUI.class,
        FreeColTextAreaUI.class,
        FreeColTextFieldUI.class,
        FreeColToolTipUI.class,
        FreeColTransparentPanelUI.class
    };


    /**
     * Initiates a new FreeCol look and feel.
     *
     * @exception FreeColException If the ui directory could not be found.
     */
    public FreeColLookAndFeel() throws FreeColException {
        super();

        setCurrentTheme(new DefaultMetalTheme() {
                @Override
                protected ColorUIResource getPrimary1() {
                    return new ColorUIResource(ImageLibrary.getColor("color.primary1.LookAndFeel"));
                }

                @Override
                protected ColorUIResource getPrimary2() {
                    return new ColorUIResource(ImageLibrary.getColor("color.backgroundSelect.LookAndFeel"));
                }

                @Override
                protected ColorUIResource getPrimary3() {
                    return new ColorUIResource(ImageLibrary.getColor("color.primary3.LookAndFeel"));
                }

                @Override
                protected ColorUIResource getSecondary1() {
                    return new ColorUIResource(ImageLibrary.getColor("color.secondary1.LookAndFeel"));
                }

                @Override
                protected ColorUIResource getSecondary2() {
                    return new ColorUIResource(ImageLibrary.getColor("color.disabled.LookAndFeel"));
                }

                @Override
                protected ColorUIResource getSecondary3() {
                    return new ColorUIResource(ImageLibrary.getColor("color.background.LookAndFeel"));
                }

                @Override
                public ColorUIResource getMenuDisabledForeground() {
                    return new ColorUIResource(ImageLibrary.getColor("color.disabledMenu.LookAndFeel"));
                }
            });
    }

    /**
     * Creates the look and feel specific defaults table.
     *
     * @return The defaults table.
     */
    @Override
    public UIDefaults getDefaults() {
        UIDefaults u = super.getDefaults();

        int offset = "FreeCol".length();
        for (Class<?> uiClass : uiClasses) {
            String name = uiClass.getName();
            int index = name.lastIndexOf("FreeCol");
            if (index >= 0) {
                index += offset;
                String shortName = name.substring(index);
                u.put(shortName, name);
                u.put(name, uiClass);
            }
        }

        // Sharing FreeColBrightPanelUI:
        try {
            u.put(BRIGHT_PANEL_UI, Class.forName(BRIGHT_PANEL_UI));
            u.put("InPortPanelUI", BRIGHT_PANEL_UI);
            u.put("CargoPanelUI", BRIGHT_PANEL_UI);
            u.put("BuildingsPanelUI", BRIGHT_PANEL_UI);
            u.put("OutsideColonyPanelUI", BRIGHT_PANEL_UI);
            u.put("WarehousePanelUI", BRIGHT_PANEL_UI);
            u.put("ConstructionPanelUI", BRIGHT_PANEL_UI);
            u.put("PopulationPanelUI", BRIGHT_PANEL_UI);
            u.put("WarehouseGoodsPanelUI", BRIGHT_PANEL_UI);
            u.put("ReportPanelUI", BRIGHT_PANEL_UI);
            u.put("ColopediaPanelUI", BRIGHT_PANEL_UI);
            u.put("TilePanelUI", BRIGHT_PANEL_UI);
            u.put("OptionGroupUI", BRIGHT_PANEL_UI);
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.WARNING, "Could not load " + BRIGHT_PANEL_UI, cnfe);
        }

        // Sharing FreeColTransparentPanelUI:
        try {
            u.put(TRANSPARENT_PANEL_UI, Class.forName(TRANSPARENT_PANEL_UI));
            u.put("MarketPanelUI", TRANSPARENT_PANEL_UI);
            u.put("EuropeCargoPanelUI", TRANSPARENT_PANEL_UI);
            u.put("ToAmericaPanelUI", TRANSPARENT_PANEL_UI);
            u.put("ToEuropePanelUI", TRANSPARENT_PANEL_UI);
            u.put("EuropeInPortPanelUI", TRANSPARENT_PANEL_UI);
            u.put("DocksPanelUI", TRANSPARENT_PANEL_UI);
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.WARNING, "Could not load " + TRANSPARENT_PANEL_UI, cnfe);
        }

        // ColorButton
        u.put("javax.swing.plaf.metal.MetalButtonUI",
            javax.swing.plaf.metal.MetalButtonUI.class);
        u.put("ColorButtonUI", "javax.swing.plaf.metal.MetalButtonUI");

        // Add cursors:
        u.put("cursor.go", ImageLibrary.getCursor());

        return u;
    }

    /**
     * Installs a FreeColLookAndFeel as the default look and feel.
     *
     * @param fclaf The {@code FreeColLookAndFeel} to install.
     * @throws FreeColException if the installation fails.
     */
    public static void install(FreeColLookAndFeel fclaf)
        throws FreeColException {
        try {
            UIManager.setLookAndFeel(fclaf);
            UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
        } catch (UnsupportedLookAndFeelException e) {
            throw new FreeColException("Look and feel install failure", e);
        }
    }

    /**
     * Set the default font in all UI elements.
     *
     * @param defaultFont A {@code Font} to use by default.
     */
    public static void installFont(Font defaultFont) {
        UIDefaults u = UIManager.getDefaults();
        java.util.Enumeration<Object> keys = u.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (u.get(key) instanceof javax.swing.plaf.FontUIResource) {
                u.put(key, defaultFont);
            }
        }
    }

    /**
     * Gets a one line description of this Look and Feel.
     *
     * @return "The default Look and Feel for FreeCol"
     */
    @Override
    public String getDescription() {
        return "The default Look and Feel for FreeCol";
    }


    /**
     * Gets the name of this Look and Feel.
     *
     * @return "FreeCol Look and Feel"
     */
    @Override
    public String getName() {
        return "FreeCol Look and Feel";
    }
}
