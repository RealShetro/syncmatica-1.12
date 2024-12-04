package shetro.syncmatica.communication.exchange;

import shetro.syncmatica.Context;
import shetro.syncmatica.RedirectFileStorage;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.communication.ClientCommunicationManager;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.communication.PacketType;
import shetro.syncmatica.litematica.LitematicManager;
import litematica.schematic.placement.SchematicPlacement;
import net.minecraft.network.PacketBuffer;

import java.io.File;
import java.io.FileNotFoundException;

public class ShareLitematicExchange extends AbstractExchange {
    private final SchematicPlacement schematicPlacement;
    private final ServerPlacement toShare;
    private final File toUpload;

    public ShareLitematicExchange(final SchematicPlacement schematicPlacement, final ExchangeTarget partner, final Context con) {
        this(schematicPlacement, partner, con, null);
    }

    public ShareLitematicExchange(final SchematicPlacement schematicPlacement, final ExchangeTarget partner, final Context con, final ServerPlacement p) {
        super(partner, con);
        this.schematicPlacement = schematicPlacement;
        toShare = p == null ? LitematicManager.getInstance().syncmaticFromSchematic(schematicPlacement) : p;
        toUpload = schematicPlacement.getSchematicFile().toFile();
    }

    @Override
    public boolean checkPacket(final String id, final PacketBuffer packetBuf) {
        if (id.equals(PacketType.REQUEST_LITEMATIC.identifier)
                || id.equals(PacketType.REGISTER_METADATA.identifier)
                || id.equals(PacketType.CANCEL_SHARE.identifier)) {
            return AbstractExchange.checkUUID(packetBuf, toShare.getId());
        }
        return false;
    }

    @Override
    public void handle(final String id, final PacketBuffer packetBuf) {
        if (id.equals(PacketType.REQUEST_LITEMATIC.identifier)) {
            packetBuf.readUniqueId();
            final UploadExchange upload;
            try {
                upload = new UploadExchange(toShare, toUpload, getPartner(), getContext());
            } catch (final FileNotFoundException e) {
                e.printStackTrace();

                return;
            }
            getManager().startExchange(upload);
            return;
        }
        if (id.equals(PacketType.REGISTER_METADATA.identifier)) {
            final RedirectFileStorage redirect = (RedirectFileStorage) getContext().getFileStorage();
            redirect.addRedirect(toUpload);
            LitematicManager.getInstance().renderSyncmatic(toShare, schematicPlacement, false);
            getContext().getSyncmaticManager().addPlacement(toShare);
            return;
        }
        if (id.equals(PacketType.CANCEL_SHARE.identifier)) {
            close(false);
        }
    }

    @Override
    public void init() {
        if (toShare == null) {
            close(false);
            return;
        }
        ((ClientCommunicationManager) getManager()).setSharingState(toShare, true);
        getContext().getSyncmaticManager().updateServerPlacement(toShare);
        getManager().sendMetaData(toShare, getPartner());
    }

    @Override
    public void onClose() {
        ((ClientCommunicationManager) getManager()).setSharingState(toShare, false);
    }
}