package shetro.syncmatica.communication.exchange;

import shetro.syncmatica.Context;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.Syncmatica;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.communication.FeatureSet;
import shetro.syncmatica.communication.PacketType;
import shetro.syncmatica.litematica.LitematicManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;

public class VersionHandshakeClient extends FeatureExchange {
    private String partnerVersion;

    public VersionHandshakeClient(final ExchangeTarget partner, final Context con) {
        super(partner, con);
    }

    @Override
    public boolean checkPacket(final String id, final PacketBuffer packetBuf) {
        return id.equals(PacketType.CONFIRM_USER.identifier)
                || id.equals(PacketType.REGISTER_VERSION.identifier)
                || super.checkPacket(id, packetBuf);
    }

    @Override
    public void handle(final String id, final PacketBuffer packetBuf) {
        if (id.equals(PacketType.REGISTER_VERSION.identifier)) {
            final String version = packetBuf.readString(32767);
            if (!getContext().checkPartnerVersion(version)) {
                // any further packets are risky so no further packets should get send
                LogManager.getLogger(VersionHandshakeClient.class).info("Denying syncmatica join due to outdated server with local version {} and server version {}", Syncmatica.VERSION, version);
                close(false);
            } else {
                partnerVersion = version;
                final FeatureSet fs = FeatureSet.fromVersionString(version);
                if (fs == null) {
                    requestFeatureSet();
                } else {
                    getPartner().setFeatureSet(fs);
                    onFeatureSetReceive();
                }
            }
        } else if (id.equals(PacketType.CONFIRM_USER.identifier)) {
            final int placementCount = packetBuf.readInt();
            for (int i = 0; i < placementCount; i++) {
                final ServerPlacement p = getManager().receiveMetaData(packetBuf, getPartner());
                getContext().getSyncmaticManager().addPlacement(p);
            }
            LogManager.getLogger(VersionHandshakeClient.class).info("Joining syncmatica server with local version {} and server version {}", Syncmatica.VERSION, partnerVersion);
            LitematicManager.getInstance().commitLoad();
            getContext().startup();
            succeed();
        } else {
            super.handle(id, packetBuf);
        }
    }

    @Override
    public void onFeatureSetReceive() {
        final PacketBuffer newBuf = new PacketBuffer(Unpooled.buffer());
        newBuf.writeString(Syncmatica.VERSION);
        getPartner().sendPacket(PacketType.REGISTER_VERSION.identifier, newBuf, getContext());
    }

    @Override
    public void init() {
        // Not required - just await message from the server
    }
}