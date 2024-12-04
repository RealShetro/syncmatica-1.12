package shetro.syncmatica.litematica_mixin;

import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SchematicPlacementManager.class)
public interface MixinSchematicPlacementManager {
    @Invoker(value = "onPrePlacementChange", remap = false)
    void preSubregionChange(SchematicPlacement schematicPlacement);

    @Invoker(value = "onPlacementRegionModified", remap = false)
    void onFinishedMoving(SchematicPlacement placement);
}