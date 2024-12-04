package shetro.syncmatica.mixin_actor;

import shetro.syncmatica.IFileStorage;
import shetro.syncmatica.RedirectFileStorage;
import shetro.syncmatica.SyncmaticManager;
import shetro.syncmatica.Syncmatica;
import shetro.syncmatica.communication.ClientCommunicationManager;
import shetro.syncmatica.communication.CommunicationManager;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.litematica.LitematicManager;
import shetro.syncmatica.litematica.ScreenHelper;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ActorNetHandlerPlayClient {
    private static ActorNetHandlerPlayClient instance;
    private static NetHandlerPlayClient netHandlerPlayClient;
    private CommunicationManager clientCommunication;
    private ExchangeTarget exTarget;

    public static ActorNetHandlerPlayClient getInstance() {
        if (instance == null) {

            instance = new ActorNetHandlerPlayClient();
        }

        return instance;
    }

    public void startEvent(final NetHandlerPlayClient netHandlerPlayClient) {
        setNetHandlerPlayClient(netHandlerPlayClient);
        startClient();
    }

    public void startClient() {
        if (netHandlerPlayClient == null) {
            throw new RuntimeException("Tried to start client before receiving a connection");
        }
        final IFileStorage data = new RedirectFileStorage();
        final SyncmaticManager man = new SyncmaticManager();
        exTarget = new ExchangeTarget(netHandlerPlayClient);
        final CommunicationManager comms = new ClientCommunicationManager(exTarget);
        Syncmatica.initClient(comms, data, man);
        clientCommunication = comms;
        ScreenHelper.init();
        LitematicManager.getInstance().setActiveContext(Syncmatica.getContext(Syncmatica.CLIENT_CONTEXT));
    }

    public void packetEvent(final NetHandlerPlayClient netHandlerPlayClient, final SPacketCustomPayload packet, final CallbackInfo ci) {
        final String id = packet.getChannelName();
        final PacketBuffer buf = packet.getBufferData();
        if (clientCommunication == null) {

            ActorNetHandlerPlayClient.getInstance().startEvent(netHandlerPlayClient);
        }
        if (packetEvent(id, buf)) {

            ci.cancel(); // prevent further unnecessary comparisons and reporting a warning
        }
    }

    public boolean packetEvent(final String id, final PacketBuffer buf) {
        if (clientCommunication.handlePacket(id)) {
            clientCommunication.onPacket(exTarget, id, buf);

            return true;
        }

        return false;
    }

    public void reset() {
        clientCommunication = null;
        exTarget = null;
        netHandlerPlayClient = null;
    }

    private static void setNetHandlerPlayClient(final NetHandlerPlayClient netHandlerPlayClient) {
        ActorNetHandlerPlayClient.netHandlerPlayClient = netHandlerPlayClient;
    }
}