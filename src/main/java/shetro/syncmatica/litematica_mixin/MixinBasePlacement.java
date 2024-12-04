package shetro.syncmatica.litematica_mixin;

import litematica.schematic.placement.BasePlacement;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BasePlacement.class)
public interface MixinBasePlacement {
    @Invoker(value = "setRotation", remap = false)
    public void setBlockRotation(Rotation rotation);

    @Invoker(value = "setMirror", remap = false)
    public void setBlockMirror(Mirror mirror);

    @Invoker(value = "setPosition", remap = false)
    public void setBlockPosition(BlockPos pos);
}