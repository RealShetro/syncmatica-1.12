package shetro.syncmatica.litematica_mixin;

import shetro.syncmatica.litematica.IIDContainer;
import shetro.syncmatica.litematica.LitematicManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import litematica.schematic.placement.SchematicPlacement;
import malilib.util.data.json.JsonUtils;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.UUID;
import javax.annotation.Nullable;

@Mixin(SchematicPlacement.class)
public abstract class MixinSchematicPlacement implements IIDContainer {
    // unsure if I can just make an assignment here so I mixin to the constructor
    @Unique
    UUID serverId;

    private MixinSchematicPlacement() {
    }

    @Inject(method = "toJson", at = @At("RETURN"), remap = false)
    public void saveUuid(final CallbackInfoReturnable<JsonObject> cir) {
        final JsonObject saveData = cir.getReturnValue();
        if (saveData != null) {
            if (serverId != null) {
                saveData.add("syncmatica_uuid", new JsonPrimitive(serverId.toString()));
            }
        }
    }

    @Inject(method = "createFromJson", at = @At("RETURN"), remap = false, cancellable = true)
    private static void loadSyncmatic(final JsonObject obj, final CallbackInfoReturnable<SchematicPlacement> cir) {
        if (JsonUtils.hasString(obj, "syncmatica_uuid")) {
            final SchematicPlacement newInstance = cir.getReturnValue();
            if (newInstance != null) {
                ((IIDContainer) newInstance).setServerId(UUID.fromString(obj.get("syncmatica_uuid").getAsString()));
                cir.setReturnValue(null);
                LitematicManager.getInstance().preLoad(newInstance);
            }
        }
    }

    @Inject(method = "<init>*", at = @At("TAIL"), remap = false)
    public void setNull(@Nullable final Path schematicFile, final BlockPos origin, final String name, final boolean enabled, final CallbackInfo ci) {
        serverId = null;
    }

    @Override
    public void setServerId(final UUID i) {
        serverId = i;
    }

    @Override
    public UUID getServerId() {
        return serverId;
    }
}