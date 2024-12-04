package shetro.syncmatica.litematica_mixin;

import shetro.syncmatica.litematica.LitematicManager;
import litematica.data.SchematicHolder;
import litematica.schematic.ISchematic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SchematicHolder.class)
public abstract class MixinSchematicHolder {
	public MixinSchematicHolder() {
	}

	@Inject(method = "removeSchematic", at = @At("RETURN"), remap = false)
	public void unloadSyncmatic(final ISchematic schematic, final CallbackInfoReturnable<Boolean> ci) {
		LitematicManager.getInstance().unrenderSchematic(schematic);
	}
}