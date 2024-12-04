package shetro.syncmatica.mixin;

import shetro.syncmatica.mixin_actor.ActorNetHandlerPlayClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void handlePacket(final SPacketCustomPayload packet, final CallbackInfo ci) {
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            return; //only execute packet on main thread
        }
        ActorNetHandlerPlayClient.getInstance().packetEvent((NetHandlerPlayClient) (Object) this, packet, ci);
    }
}