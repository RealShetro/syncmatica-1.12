package shetro.syncmatica.communication;

import shetro.syncmatica.Context;
import shetro.syncmatica.Feature;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.Syncmatica;
import shetro.syncmatica.communication.exchange.DownloadExchange;
import shetro.syncmatica.communication.exchange.Exchange;
import shetro.syncmatica.communication.exchange.VersionHandshakeClient;
import shetro.syncmatica.extended_core.PlayerIdentifier;
import shetro.syncmatica.litematica.LitematicManager;
import shetro.syncmatica.litematica.ScreenHelper;
import shetro.syncmatica.litematica.gui.SyncmaticaServerPlacementListScreen;
import shetro.syncmatica.mixin_actor.ActorNetHandlerPlayClient;
import malilib.overlay.message.Message;
import malilib.gui.util.GuiUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraft.client.gui.GuiScreen;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public class ClientCommunicationManager extends CommunicationManager {
    private final ExchangeTarget server;
    private final Collection<ServerPlacement> sharing;

    public ClientCommunicationManager(final ExchangeTarget server) {
        super();
        this.server = server;
        broadcastTargets.add(server);
        sharing = new HashSet<>();
    }

    public ExchangeTarget getServer() {
        return server;
    }

    @Override
    protected void handle(final ExchangeTarget source, final String id, final PacketBuffer packetBuf) {
        if (id.equals(PacketType.REGISTER_METADATA.identifier)) {
            final ServerPlacement placement = receiveMetaData(packetBuf, source);
            context.getSyncmaticManager().addPlacement(placement);
            return;
        }
        if (id.equals(PacketType.REMOVE_SYNCMATIC.identifier)) {
            final UUID placementId = packetBuf.readUniqueId();
            final ServerPlacement placement = context.getSyncmaticManager().getPlacement(placementId);
            if (placement != null) {
                final Exchange modifier = getModifier(placement);
                if (modifier != null) {
                    modifier.close(false);
                    notifyClose(modifier);
                }
                context.getSyncmaticManager().removePlacement(placement);
                GuiScreen screen = GuiUtils.getCurrentScreen();
                if (screen instanceof SyncmaticaServerPlacementListScreen) {
                	SyncmaticaServerPlacementListScreen serverPlacementScreen = (SyncmaticaServerPlacementListScreen) screen;
                    serverPlacementScreen.getListWidget().refreshEntries();
                    serverPlacementScreen.getListWidget().clearSelection();
                    serverPlacementScreen.onSelectionChange(null);
                }
                if (LitematicManager.getInstance().isRendered(placement)) {
                    LitematicManager.getInstance().unrenderSyncmatic(placement);
                }
            }
            return;
        }
        if (id.equals(PacketType.MODIFY.identifier)) {
            final UUID placementId = packetBuf.readUniqueId();
            final ServerPlacement toModify = context.getSyncmaticManager().getPlacement(placementId);
            receivePositionData(toModify, packetBuf, source);
            if (source.getFeatureSet().hasFeature(Feature.CORE_EX)) {
                final PlayerIdentifier lastModifiedBy = context.getPlayerIdentifierProvider().createOrGet(
                        packetBuf.readUniqueId(),
                        packetBuf.readString(32767)
                );

                toModify.setLastModifiedBy(lastModifiedBy);
            }
            LitematicManager.getInstance().updateRendered(toModify);
            context.getSyncmaticManager().updateServerPlacement(toModify);
            return;
        }
        if (id.equals(PacketType.MESSAGE.identifier)) {
            final int type = mapMessageType(MessageType.valueOf(packetBuf.readString(32767)));
            final String text = packetBuf.readString(32767);
            ScreenHelper.ifPresent(s -> s.addMessage(type, text));
            return;
        }
        if (id.equals(PacketType.REGISTER_VERSION.identifier)) {
            LitematicManager.clear();
            Syncmatica.restartClient();
            ActorNetHandlerPlayClient.getInstance().packetEvent(id, packetBuf);
        }
    }

    @Override
    protected void handleExchange(final Exchange exchange) {
        if (exchange instanceof DownloadExchange && exchange.isSuccessful()) {
            LitematicManager.getInstance().renderSyncmatic(((DownloadExchange) exchange).getPlacement());
        }
    }

    @Override
    public void setDownloadState(final ServerPlacement syncmatic, final boolean state) {
        downloadState.put(syncmatic.getHash(), state);
        if (state || LitematicManager.getInstance().isRendered(syncmatic)) { //change client behavior so that the Load button doesn't show up naturally
            context.getSyncmaticManager().updateServerPlacement(syncmatic);
        }
    }

    public void setSharingState(final ServerPlacement placement, final boolean state) {
        if (state) {
            sharing.add(placement);
        } else {
            sharing.remove(placement);
        }
    }

    public boolean getSharingState(final ServerPlacement placement) {
        return sharing.contains(placement);
    }

    @Override
    public void setContext(final Context con) {
        super.setContext(con);
        final VersionHandshakeClient hi = new VersionHandshakeClient(server, context);
        startExchangeUnchecked(hi);
    }

    private int mapMessageType(final MessageType m) {
        switch (m) {
            case SUCCESS:
                return Message.SUCCESS;
            case WARNING:
                return Message.WARNING;
            case ERROR:
                return Message.ERROR;
            default:
                return Message.INFO;
        }
    }
}