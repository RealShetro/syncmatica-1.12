package shetro.syncmatica.mixin;

import shetro.syncmatica.Context;
import shetro.syncmatica.Syncmatica;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.communication.ServerCommunicationManager;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {
    @Unique
    private ExchangeTarget exTarget = null;
    @Unique
    private ServerCommunicationManager comManager = null;

    @Shadow
    public EntityPlayerMP player;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onConnect(final MinecraftServer server, final NetworkManager connection, final EntityPlayerMP player, final CallbackInfo ci) {
        operateComms(sm -> sm.onPlayerJoin(getExchangeTarget(), player));
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    public void onDisconnect(final ITextComponent reason, final CallbackInfo ci) {
        operateComms(sm -> sm.onPlayerLeave(getExchangeTarget()));
    }

    @Inject(method = "processCustomPayload", at = @At("HEAD"))
    public void processCustomPayload(final CPacketCustomPayload packet, final CallbackInfo ci) {
    	final String id = packet.getChannelName();
    	if (comManager.handlePacket(id)) {
            PacketThreadUtil.checkThreadAndEnqueue(packet, (NetHandlerPlayServer) (Object) this, player.getServerWorld());
            final PacketBuffer packetBuf = packet.getBufferData();
            operateComms(sm -> sm.onPacket(getExchangeTarget(), id, packetBuf));
        }
    }

    private ExchangeTarget getExchangeTarget() {
        if (exTarget == null) {
            exTarget = new ExchangeTarget((NetHandlerPlayServer) (Object) this);
        }
        return exTarget;
    }

    private void operateComms(final Consumer<ServerCommunicationManager> operation) {
        if (comManager == null) {
            final Context con = Syncmatica.getContext(Syncmatica.SERVER_CONTEXT);
            if (con != null) {
                comManager = (ServerCommunicationManager) con.getCommunicationManager();
            }
        }
        if (comManager != null) {
            operation.accept(comManager);
        }
    }
}