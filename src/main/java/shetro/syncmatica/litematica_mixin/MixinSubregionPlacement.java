package shetro.syncmatica.litematica_mixin;

import litematica.schematic.placement.SubRegionPlacement;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SubRegionPlacement.class)
public interface MixinSubregionPlacement {
    @Accessor(value = "defaultPos", remap = false)
    BlockPos getDefaultPosition();

    @Invoker(value = "resetToOriginalValues", remap = false)
    void reset();
}