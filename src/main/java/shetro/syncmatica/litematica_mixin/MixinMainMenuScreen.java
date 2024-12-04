package shetro.syncmatica.litematica_mixin;

import shetro.syncmatica.Context;
import shetro.syncmatica.litematica.LitematicManager;
import shetro.syncmatica.litematica.gui.ButtonListenerChangeMenu;
import shetro.syncmatica.litematica.gui.MainMenuButtonType;
import litematica.gui.MainMenuScreen;
import litematica.gui.util.LitematicaIcons;
import malilib.gui.BaseScreen;
import malilib.gui.widget.button.GenericButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MainMenuScreen.class)
public class MixinMainMenuScreen extends BaseScreen {
    public GenericButton viewSyncmaticsScreenButton;
	public GenericButton materialGatheringsScreenButton;

    @Final
    @Shadow(remap = false)
    public GenericButton configScreenButton;
    @Shadow(remap = false)
    public int equalWidthWidgetMaxWidth;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    public void init(final CallbackInfo ci) {
    	final Context con = LitematicManager.getInstance().getActiveContext();
        final boolean buttonsEnabled = con != null && con.isStarted();
    	this.viewSyncmaticsScreenButton = GenericButton.create("syncmatica.gui.button.view_syncmatics", LitematicaIcons.DUMMY);
        this.viewSyncmaticsScreenButton.setEnabled(buttonsEnabled);
        this.viewSyncmaticsScreenButton.setActionListener(() -> ButtonListenerChangeMenu.actionPerformedWithButton(MainMenuButtonType.VIEW_SYNCMATICS, this));
        this.materialGatheringsScreenButton = GenericButton.create("syncmatica.gui.button.material_gatherings", LitematicaIcons.DUMMY);
        this.materialGatheringsScreenButton.setEnabled(buttonsEnabled);
        this.materialGatheringsScreenButton.setEnabled(false);
        this.materialGatheringsScreenButton.setActionListener(() -> ButtonListenerChangeMenu.actionPerformedWithButton(MainMenuButtonType.MATERIAL_GATHERINGS, this));
    }

    @Inject(method = "reAddActiveWidgets", at = @At("RETURN"), remap = false)
    public void reAddActiveWidgets(final CallbackInfo ci) {
    	int width = this.equalWidthWidgetMaxWidth + 10;
        this.viewSyncmaticsScreenButton.setAutomaticWidth(false);
        this.materialGatheringsScreenButton.setAutomaticWidth(false);
        this.addWidget(this.viewSyncmaticsScreenButton);
        this.addWidget(this.materialGatheringsScreenButton);
        this.viewSyncmaticsScreenButton.setWidth(width);
        this.materialGatheringsScreenButton.setWidth(width);
    }

    @Inject(method = "updateWidgetPositions", at = @At("RETURN"), remap = false)
    public void updateWidgetPositions(final CallbackInfo ci) {
    	final int x = this.configScreenButton.getRight() + 20;
        int y = 30;
        this.viewSyncmaticsScreenButton.setPosition(x, y);
        y += 22;
        this.materialGatheringsScreenButton.setPosition(x, y);
    }
}