package shetro.syncmatica.communication;

import shetro.syncmatica.Feature;
import shetro.syncmatica.LocalLitematicState;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.communication.exchange.*;
import shetro.syncmatica.extended_core.PlayerIdentifier;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class ServerCommunicationManager extends CommunicationManager {
    private final Map<UUID, List<ServerPlacement>> downloadingFile = new HashMap<>();
    private final Map<ExchangeTarget, EntityPlayerMP> playerMap = new HashMap<>();

    public ServerCommunicationManager() {
        super();
    }

    public GameProfile getGameProfile(final ExchangeTarget exchangeTarget) {
        return playerMap.get(exchangeTarget).getGameProfile();
    }

    public void sendMessage(final ExchangeTarget client, final MessageType type, final String identifier) {
        if (client.getFeatureSet().hasFeature(Feature.MESSAGE)) {
            final PacketBuffer newPacketBuf = new PacketBuffer(Unpooled.buffer());
            newPacketBuf.writeString(type.toString());
            newPacketBuf.writeString(identifier);
            client.sendPacket(PacketType.MESSAGE.identifier, newPacketBuf, context);
        } else if (playerMap.containsKey(client)) {
            final EntityPlayerMP player = playerMap.get(client);
            player.sendMessage(new TextComponentTranslation("Syncmatica " + type.toString() + " " + identifier));
        }
    }

    public void onPlayerJoin(final ExchangeTarget newPlayer, final EntityPlayerMP player) {
        final VersionHandshakeServer hi = new VersionHandshakeServer(newPlayer, context);
        playerMap.put(newPlayer, player);
        final GameProfile profile = player.getGameProfile();
        context.getPlayerIdentifierProvider().updateName(profile.getId(), profile.getName());
        startExchangeUnchecked(hi);
    }

    public void onPlayerLeave(final ExchangeTarget oldPlayer) {
        final Collection<Exchange> potentialMessageTarget = oldPlayer.getExchanges();
        if (potentialMessageTarget != null) {
            for (final Exchange target : potentialMessageTarget) {
                target.close(false);
                handleExchange(target);
            }
        }
        broadcastTargets.remove(oldPlayer);
        playerMap.remove(oldPlayer);
    }

    @Override
    protected void handle(final ExchangeTarget source, final String id, final PacketBuffer packetBuf) {
        if (id.equals(PacketType.REQUEST_LITEMATIC.identifier)) {
            final UUID syncmaticaId = packetBuf.readUniqueId();
            final ServerPlacement placement = context.getSyncmaticManager().getPlacement(syncmaticaId);
            if (placement == null) {
                return;
            }
            final File toUpload = context.getFileStorage().getLocalLitematic(placement);
            final UploadExchange upload;
            try {
                upload = new UploadExchange(placement, toUpload, source, context);
            } catch (final FileNotFoundException e) {
                // should be fine
                e.printStackTrace();
                return;
            }
            startExchange(upload);
            return;
        }
        if (id.equals(PacketType.REGISTER_METADATA.identifier)) {
            final ServerPlacement placement = receiveMetaData(packetBuf, source);
            if (context.getSyncmaticManager().getPlacement(placement.getId()) != null) {
                cancelShare(source, placement);

                return;
            }

            // when the client does not communicate the owner
            final GameProfile profile = playerMap.get(source).getGameProfile();
            final PlayerIdentifier playerIdentifier = context.getPlayerIdentifierProvider().createOrGet(profile);
            if (!placement.getOwner().equals(playerIdentifier)) {
                placement.setOwner(playerIdentifier);
                placement.setLastModifiedBy(playerIdentifier);
            }

            if (!context.getFileStorage().getLocalState(placement).isLocalFileReady()) {
                // special edge case because files are transmitted by placement rather than file names/hashes
                if (context.getFileStorage().getLocalState(placement) == LocalLitematicState.DOWNLOADING_LITEMATIC) {
                    downloadingFile.computeIfAbsent(placement.getHash(), key -> new ArrayList<>()).add(placement);
                    return;
                }
                try {
                    download(placement, source);
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                return;
            }

            addPlacement(source, placement);

            return;
        }
        if (id.equals(PacketType.REMOVE_SYNCMATIC.identifier)) {
            final UUID placementId = packetBuf.readUniqueId();
            final ServerPlacement placement = context.getSyncmaticManager().getPlacement(placementId);
            if (placement != null) {
                final Exchange modifier = getModifier(placement);
                if (modifier != null) {
                    modifier.close(true);
                    notifyClose(modifier);
                }
                context.getSyncmaticManager().removePlacement(placement);
                for (final ExchangeTarget client : broadcastTargets) {
                    final PacketBuffer newPacketBuf = new PacketBuffer(Unpooled.buffer());
                    newPacketBuf.writeUniqueId(placement.getId());
                    client.sendPacket(PacketType.REMOVE_SYNCMATIC.identifier, newPacketBuf, context);
                }
            }
        }
        if (id.equals(PacketType.MODIFY_REQUEST.identifier)) {
            final UUID placementId = packetBuf.readUniqueId();
            final ModifyExchangeServer modifier = new ModifyExchangeServer(placementId, source, context);
            startExchange(modifier);
        }
    }

    @Override
    protected void handleExchange(final Exchange exchange) {
        if (exchange instanceof DownloadExchange) {
            final ServerPlacement p = ((DownloadExchange) exchange).getPlacement();

            if (exchange.isSuccessful()) {
                addPlacement(exchange.getPartner(), p);
                if (downloadingFile.containsKey(p.getHash())) {
                    for (final ServerPlacement placement : downloadingFile.get(p.getHash())) {
                        addPlacement(exchange.getPartner(), placement);
                    }
                }
            } else {
                cancelShare(exchange.getPartner(), p);
                if (downloadingFile.containsKey(p.getHash())) {
                    for (final ServerPlacement placement : downloadingFile.get(p.getHash())) {
                        cancelShare(exchange.getPartner(), placement);
                    }
                }
            }

            downloadingFile.remove(p.getHash());
            return;
        }
        if (exchange instanceof VersionHandshakeServer && exchange.isSuccessful()) {
            broadcastTargets.add(exchange.getPartner());
        }
        if (exchange instanceof ModifyExchangeServer && exchange.isSuccessful()) {
            final ServerPlacement placement = ((ModifyExchangeServer) exchange).getPlacement();
            for (final ExchangeTarget client : broadcastTargets) {
                if (client.getFeatureSet().hasFeature(Feature.MODIFY)) {
                    // client supports modify so just send modify
                    final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
                    buf.writeUniqueId(placement.getId());
                    putPositionData(placement, buf, client);
                    if (client.getFeatureSet().hasFeature(Feature.CORE_EX)) {
                        buf.writeUniqueId(placement.getLastModifiedBy().uuid);
                        buf.writeString(placement.getLastModifiedBy().getName());
                    }
                    client.sendPacket(PacketType.MODIFY.identifier, buf, context);
                } else {
                    // client doesn't support modification so
                    // send data and then
                    final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
                    buf.writeUniqueId(placement.getId());
                    client.sendPacket(PacketType.REMOVE_SYNCMATIC.identifier, buf, context);
                    final PacketBuffer buf2 = new PacketBuffer(Unpooled.buffer());
                    putMetaData(placement, buf2, client);
                    client.sendPacket(PacketType.REGISTER_METADATA.identifier, buf2, context);
                }
            }
        }
    }

    private void addPlacement(final ExchangeTarget t, final ServerPlacement placement) {
        if (context.getSyncmaticManager().getPlacement(placement.getId()) != null) {
            cancelShare(t, placement);
            return;
        }
        context.getSyncmaticManager().addPlacement(placement);
        for (final ExchangeTarget target : broadcastTargets) {
            sendMetaData(placement, target);
        }
    }

    private void cancelShare(final ExchangeTarget source, final ServerPlacement placement) {
        final PacketBuffer packetBuffer = new PacketBuffer(Unpooled.buffer());
        packetBuffer.writeUniqueId(placement.getId());
        source.sendPacket(PacketType.CANCEL_SHARE.identifier, packetBuffer, context);
    }
}