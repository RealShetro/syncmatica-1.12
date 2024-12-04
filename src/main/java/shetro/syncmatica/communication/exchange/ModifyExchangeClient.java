package shetro.syncmatica.communication.exchange;

import shetro.syncmatica.Context;
import shetro.syncmatica.Feature;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.communication.PacketType;
import shetro.syncmatica.litematica_mixin.MixinBasePlacement;
import shetro.syncmatica.litematica.LitematicManager;
import shetro.syncmatica.litematica.ScreenHelper;
import litematica.schematic.placement.SchematicPlacement;
import malilib.overlay.message.Message;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

public class ModifyExchangeClient extends AbstractExchange {
    //bad practice but valid for communication with deprecated systems
    private boolean expectRemove = false;

    private final ServerPlacement placement;
    private final SchematicPlacement litematic;

    public ModifyExchangeClient(final ServerPlacement placement, final ExchangeTarget partner, final Context con) {
        super(partner, con);
        this.placement = placement;
        litematic = LitematicManager.getInstance().schematicFromSyncmatic(placement);
    }

    @Override
    public boolean checkPacket(final String id, final PacketBuffer packetBuf) {
        if (id.equals(PacketType.MODIFY_REQUEST_DENY.identifier)
                || id.equals(PacketType.MODIFY_REQUEST_ACCEPT.identifier)
                || (expectRemove && id.equals(PacketType.REMOVE_SYNCMATIC.identifier))) {
            return AbstractExchange.checkUUID(packetBuf, placement.getId());
        }
        return false;
    }

    @Override
    public void handle(final String id, final PacketBuffer packetBuf) {
        if (id.equals(PacketType.MODIFY_REQUEST_DENY.identifier)) {
            packetBuf.readUniqueId();
            close(false);
            if (!litematic.isLocked()) {
                litematica.data.DataManager.INSTANCE.getSchematicPlacementManager().setOrigin(litematic, placement.getPosition());
                ((MixinBasePlacement) litematic).setBlockRotation(placement.getRotation());
                ((MixinBasePlacement) litematic).setBlockMirror(placement.getMirror());
                litematic.toggleLocked();
            }
            ScreenHelper.ifPresent(s -> s.addMessage(Message.SUCCESS, "syncmatica.error.modification_deny"));
        } else if (id.equals(PacketType.MODIFY_REQUEST_ACCEPT.identifier)) {
            packetBuf.readUniqueId();
            acceptModification();
        } else if (id.equals(PacketType.REMOVE_SYNCMATIC.identifier)) {
            packetBuf.readUniqueId();
            final ShareLitematicExchange legacyModify = new ShareLitematicExchange(litematic, getPartner(), getContext(), placement);
            getContext().getCommunicationManager().startExchange(legacyModify);
            succeed(); // the adding portion of this is handled by the ShareLitematicExchange
        }
    }

    @Override
    public void init() {
        if (getContext().getCommunicationManager().getModifier(placement) != null) {
            close(false);
            return;
        }
        getContext().getCommunicationManager().setModifier(placement, this);
        if (getPartner().getFeatureSet().hasFeature(Feature.MODIFY)) {
            final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            buf.writeUniqueId(placement.getId());
            getPartner().sendPacket(PacketType.MODIFY_REQUEST.identifier, buf, getContext());
        } else {
            acceptModification();
        }
    }

    private void acceptModification() {
        if (litematic.isLocked()) {
            litematic.toggleLocked();
        }
        ScreenHelper.ifPresent(s -> s.addMessage(Message.SUCCESS, "syncmatica.success.modification_accepted"));
        getContext().getSyncmaticManager().updateServerPlacement(placement);
    }

    public void conclude() {
        LitematicManager.getInstance().updateServerPlacement(litematic, placement);
        sendFinish();
        if (!litematic.isLocked()) {
            litematic.toggleLocked();
        }
        getContext().getSyncmaticManager().updateServerPlacement(placement);
    }

    private void sendFinish() {
        if (getPartner().getFeatureSet().hasFeature(Feature.MODIFY)) {
            final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            buf.writeUniqueId(placement.getId());
            getContext().getCommunicationManager().putPositionData(placement, buf, getPartner());
            getPartner().sendPacket(PacketType.MODIFY_FINISH.identifier, buf, getContext());
            succeed();
            getContext().getCommunicationManager().notifyClose(this);
        } else {
            final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            buf.writeUniqueId(placement.getId());
            getPartner().sendPacket(PacketType.REMOVE_SYNCMATIC.identifier, buf, getContext());
            expectRemove = true;
        }
    }

    @Override
    protected void sendCancelPacket() {
        if (getPartner().getFeatureSet().hasFeature(Feature.MODIFY)) {
            final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            buf.writeUniqueId(placement.getId());
            getContext().getCommunicationManager().putPositionData(placement, buf, getPartner());
            getPartner().sendPacket(PacketType.MODIFY_FINISH.identifier, buf, getContext());
        }
    }

    @Override
    protected void onClose() {
        if (getContext().getCommunicationManager().getModifier(placement) == this) {
            getContext().getCommunicationManager().setModifier(placement, null);
        }
    }
}