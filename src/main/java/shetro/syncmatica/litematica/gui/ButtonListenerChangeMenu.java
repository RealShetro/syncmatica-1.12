package shetro.syncmatica.litematica.gui;

import malilib.gui.BaseScreen;
import net.minecraft.client.gui.GuiScreen;
import org.apache.logging.log4j.LogManager;

public class ButtonListenerChangeMenu {
    public static void actionPerformedWithButton(final MainMenuButtonType type, final GuiScreen parent) {
        BaseScreen gui = null;
        switch (type) {
            case MATERIAL_GATHERINGS:
                LogManager.getLogger().info("Opened Material Gatherings GUI - currently unsupported operation");
                break;
            case VIEW_SYNCMATICS:
                gui = new SyncmaticaServerPlacementListScreen();
                break;
            default:
                break;
        }
        if (gui != null) {
            gui.setParent(parent);
            BaseScreen.openScreen(gui);
        }
    }
}