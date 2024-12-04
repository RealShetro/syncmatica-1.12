package shetro.syncmatica.mixin;

import shetro.syncmatica.Syncmatica;
import shetro.syncmatica.litematica.LitematicManager;
import shetro.syncmatica.litematica.ScreenHelper;
import shetro.syncmatica.mixin_actor.ActorNetHandlerPlayClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void shutdownSyncmatica(@Nullable final WorldClient world, final String loadingMessage, final CallbackInfo ci) {
    	if (world == null) {
            ScreenHelper.close();
            Syncmatica.shutdown();
            LitematicManager.clear();
            ActorNetHandlerPlayClient.getInstance().reset();
        }
    }
}