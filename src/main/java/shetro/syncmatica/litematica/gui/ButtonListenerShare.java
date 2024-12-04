package shetro.syncmatica.litematica.gui;

import shetro.syncmatica.Context;
import shetro.syncmatica.Feature;
import shetro.syncmatica.communication.ClientCommunicationManager;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.communication.exchange.ShareLitematicExchange;
import shetro.syncmatica.litematica.LitematicManager;
import litematica.schematic.placement.SchematicPlacement;
import malilib.gui.BaseScreen;
import malilib.overlay.message.MessageDispatcher;
import malilib.gui.widget.button.GenericButton;

public class ButtonListenerShare {
    public static boolean sharePlacement(final GenericButton button, final SchematicPlacement schematicPlacement) {
        if (LitematicManager.getInstance().isSyncmatic(schematicPlacement)) {
            return false;
        }
        if (!BaseScreen.isShiftDown()) {
        	MessageDispatcher.error("syncmatica.error.share_without_shift");
            return false;
        }
        button.setEnabled(false);
        final Context con = LitematicManager.getInstance().getActiveContext();
        final ExchangeTarget server = ((ClientCommunicationManager) con.getCommunicationManager()).getServer();
        if (!server.getFeatureSet().hasFeature(Feature.CORE_EX) && schematicPlacement.isRegionPlacementModified()) {
            MessageDispatcher.error("syncmatica.error.share_modified_subregions");
            return false;
        }
        final ShareLitematicExchange ex = new ShareLitematicExchange(schematicPlacement, server, con);
        con.getCommunicationManager().startExchange(ex);
        return true;
    }
}
