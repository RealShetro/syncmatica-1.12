package shetro.syncmatica.litematica_mixin;

import shetro.syncmatica.Context;
import shetro.syncmatica.Feature;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.communication.ClientCommunicationManager;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.communication.exchange.ModifyExchangeClient;
import shetro.syncmatica.litematica.LitematicManager;
import shetro.syncmatica.litematica.ScreenHelper;
import litematica.gui.SchematicPlacementSettingsScreen;
import litematica.schematic.placement.SchematicPlacement;
import malilib.gui.BaseScreen;
import malilib.gui.widget.button.OnOffButton;
import malilib.overlay.message.MessageDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SchematicPlacementSettingsScreen.class)
public abstract class MixinSchematicPlacementSettingsScreen extends BaseScreen {
	@Final
	@Shadow(remap = false)
	public OnOffButton toggleLockedButton;
    @Final
    @Shadow(remap = false)
    public SchematicPlacement placement;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    public void initGui(final SchematicPlacement placement, final CallbackInfo ci) {
        if (!LitematicManager.getInstance().isSyncmatic(placement)) {
            return;
        }
        this.toggleLockedButton.setActionListener((mouseButton, buttonWidget) -> {
            if (placement.isLocked()) {
                requestModification();
            } else {
                finishModification();
            }
            return true;
        });
        ScreenHelper.ifPresent(s -> s.setCurrentGui(this));
    }

    private void requestModification() {
        final Context context = LitematicManager.getInstance().getActiveContext();
        final ExchangeTarget server = ((ClientCommunicationManager) context.getCommunicationManager()).getServer();
        final ServerPlacement serverPlacement = LitematicManager.getInstance().syncmaticFromSchematic(placement);
        if (!server.getFeatureSet().hasFeature(Feature.CORE_EX) && placement.isRegionPlacementModified()) {
        	MessageDispatcher.error("syncmatica.error.share_modified_subregions");
            return;
        }
        final ModifyExchangeClient modifyExchange = new ModifyExchangeClient(serverPlacement, server, context);
        context.getCommunicationManager().startExchange(modifyExchange);
    }

    private void finishModification() {
        final Context context = LitematicManager.getInstance().getActiveContext();
        final ExchangeTarget server = ((ClientCommunicationManager) context.getCommunicationManager()).getServer();
        if (!server.getFeatureSet().hasFeature(Feature.CORE_EX) && placement.isRegionPlacementModified()) {
        	MessageDispatcher.error("syncmatica.error.share_modified_subregions");
            return;
        }
        final ServerPlacement serverPlacement = LitematicManager.getInstance().syncmaticFromSchematic(placement);
        final ModifyExchangeClient modifyExchange = (ModifyExchangeClient) context.getCommunicationManager().getModifier(serverPlacement);
        if (modifyExchange != null) {
            modifyExchange.conclude();
        }
    }
}