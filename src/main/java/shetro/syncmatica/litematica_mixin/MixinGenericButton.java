package shetro.syncmatica.litematica_mixin;

import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.ButtonActionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GenericButton.class)
public interface MixinGenericButton {
    @Accessor(value = "actionListener", remap = false)
    ButtonActionListener getActionListener();
}