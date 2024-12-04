package shetro.syncmatica.litematica_mixin;

import shetro.syncmatica.Context;
import shetro.syncmatica.litematica.LitematicManager;
import shetro.syncmatica.litematica.gui.ButtonListenerShare;
import litematica.gui.SchematicPlacementsListScreen;
import litematica.gui.widget.list.entry.SchematicPlacementEntryWidget;
import litematica.schematic.placement.SchematicPlacement;
import malilib.gui.BaseScreen;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.ButtonActionListener;
import malilib.gui.widget.BaseWidget;
import malilib.gui.widget.IconWidget;
import malilib.gui.widget.list.entry.BaseOrderableListEditEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SchematicPlacementEntryWidget.class)
public abstract class MixinSchematicPlacementEntryWidget extends BaseOrderableListEditEntryWidget<SchematicPlacement> {
	public GenericButton shareButton;

    @Final
    @Shadow(remap = false)
    public IconWidget lockedIcon;
    @Final
    @Shadow(remap = false)
    public IconWidget modificationNoticeIcon;
    @Shadow(remap = false)
    public int buttonsStartX;
    @Final
    @Shadow(remap = false)
    public SchematicPlacement placement;

    protected MixinSchematicPlacementEntryWidget(final SchematicPlacement placement, final DataListEntryWidgetData constructData, final SchematicPlacementsListScreen screen) {
        super(placement, constructData);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void addUploadButton(final SchematicPlacement placement, final DataListEntryWidgetData constructData, final SchematicPlacementsListScreen screen, final CallbackInfo ci) {
        int i = 0;
        if (LitematicManager.getInstance().isSyncmatic(placement)) {
            for (final BaseWidget base : subWidgets) {
                if (base instanceof GenericButton) {
                    final GenericButton button = (GenericButton) base;
                    if (++i == 1) {
                        final ButtonActionListener oldAction = ((MixinGenericButton) button).getActionListener();
                        button.setActionListener((mouseButton, buttonWidget) -> {
                            if (BaseScreen.isShiftDown()) {
                                LitematicManager.getInstance().unrenderSchematicPlacement(placement);
                                return false;
                            }
                            oldAction.actionPerformedWithButton(mouseButton, buttonWidget);
                            return true;
                        });

                    }
                }
            }
        }

        this.shareButton = GenericButton.create("syncmatica.gui.button.share");
        this.shareButton.setActionListener(() -> ButtonListenerShare.sharePlacement(this.shareButton, placement));
        final Context con = LitematicManager.getInstance().getActiveContext();
        final boolean buttonEnabled = con != null && con.isStarted() && !LitematicManager.getInstance().isSyncmatic(placement);
        this.shareButton.setEnabled(buttonEnabled);
    }

    @Inject(method = "reAddSubWidgets", at = @At("TAIL"), remap = false)
    public void reAddSubWidgets(final CallbackInfo ci) {
    	((SchematicPlacementEntryWidget) (Object) this).addWidget(this.shareButton);
    }

    @Inject(method = "updateSubWidgetPositions", at = @At("TAIL"), remap = false)
    public void updateSubWidgetPositions(final CallbackInfo ci) {
    	this.shareButton.centerVerticallyInside((SchematicPlacementEntryWidget) (Object) this);
        this.shareButton.setRight(this.modificationNoticeIcon.getX() + 12);
        this.modificationNoticeIcon.setRight(this.shareButton.getX() - 2);
        this.lockedIcon.setRight(this.modificationNoticeIcon.getX() - 1);
        this.buttonsStartX = this.lockedIcon.getX() - 1;
    }
}