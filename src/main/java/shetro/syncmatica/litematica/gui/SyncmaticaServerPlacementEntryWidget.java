package shetro.syncmatica.litematica.gui;

import shetro.syncmatica.Context;
import shetro.syncmatica.LocalLitematicState;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.communication.ClientCommunicationManager;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.communication.PacketType;
import shetro.syncmatica.litematica.LitematicManager;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import malilib.render.text.StyledTextLine;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

public class SyncmaticaServerPlacementEntryWidget extends BaseDataListEntryWidget<ServerPlacement> {
	protected final GenericButton removeButton;
	protected final GenericButton materialGatheringButton;
	protected final GenericButton downloadingButton;
	protected final GenericButton downloadButton;
	protected final GenericButton loadButton;
	protected final GenericButton unloadButton;
    private final ServerPlacement placement;

    public SyncmaticaServerPlacementEntryWidget(ServerPlacement schematic, DataListEntryWidgetData constructData) {
        super(schematic, constructData);

        placement = schematic;

        this.removeButton = GenericButton.create("syncmatica.gui.button.remove", this::remove);

        this.materialGatheringButton = GenericButton.create("syncmatica.gui.button.material_gathering_placement", this::materialGathering);
        this.materialGatheringButton.setEnabled(false);

        this.downloadingButton = GenericButton.create("syncmatica.gui.button.downloading");
        this.downloadButton = GenericButton.create("syncmatica.gui.button.download", this::download);
        this.loadButton = GenericButton.create("syncmatica.gui.button.load", this::load);
        this.unloadButton = GenericButton.create("syncmatica.gui.button.unload", this::unload);

        this.setText(StyledTextLine.parseFirstLine(placement.getName()));
    }

    @Override
    public void reAddSubWidgets() {
        super.reAddSubWidgets();

        this.addWidget(this.removeButton);
        this.addWidget(this.materialGatheringButton);
        this.addWidgetIf(this.downloadingButton, LitematicManager.getInstance().getActiveContext().getCommunicationManager().getDownloadState(placement));
        final Context con = LitematicManager.getInstance().getActiveContext();
        final LocalLitematicState state = con.getFileStorage().getLocalState(placement);
        this.addWidgetIf(this.downloadButton, !state.isLocalFileReady() && state.isReadyForDownload());
        this.addWidgetIf(this.loadButton, !LitematicManager.getInstance().isRendered(placement) && state.isLocalFileReady());
        this.addWidgetIf(this.unloadButton, LitematicManager.getInstance().isRendered(placement));
    }

    @Override
    public void updateSubWidgetPositions() {
        super.updateSubWidgetPositions();

        this.removeButton.centerVerticallyInside(this);
        this.materialGatheringButton.centerVerticallyInside(this);
        this.downloadingButton.centerVerticallyInside(this);
        this.downloadButton.centerVerticallyInside(this);
        this.loadButton.centerVerticallyInside(this);
        this.unloadButton.centerVerticallyInside(this);

        this.removeButton.setRight(this.getRight() - 2);
        this.materialGatheringButton.setRight(this.removeButton.getX() - 1);
        int x = this.materialGatheringButton.getX() - 1;
        this.downloadingButton.setRight(x);
        this.downloadButton.setRight(x);
        this.loadButton.setRight(x);
        this.unloadButton.setRight(x);
    }

    public static boolean serverPlacementSearchFilter(ServerPlacement entry, List<String> searchTerms) {
        for (String searchTerm : searchTerms) {
            if (entry.getName().toLowerCase(Locale.ROOT).contains(searchTerm)) {
                return true;
            }
        }

        return false;
    }

    protected void remove() {
        final Context con = LitematicManager.getInstance().getActiveContext();
        final ExchangeTarget server = ((ClientCommunicationManager) con.getCommunicationManager()).getServer();
        final PacketBuffer packetBuf = new PacketBuffer(Unpooled.buffer());
        packetBuf.writeUniqueId(placement.getId());
        server.sendPacket(PacketType.REMOVE_SYNCMATIC.identifier, packetBuf, LitematicManager.getInstance().getActiveContext());
    }

    protected void materialGathering() {
    	this.materialGatheringButton.setEnabled(false);
        LogManager.getLogger().info("Opened Material Gatherings GUI - currently unsupported operation");
    }

    protected void download() {
    	final Context con = LitematicManager.getInstance().getActiveContext();
        final ExchangeTarget server = ((ClientCommunicationManager) con.getCommunicationManager()).getServer();
        if (con.getCommunicationManager().getDownloadState(placement)) {
            return;
        }
        try {
            con.getCommunicationManager().download(placement, server);
        } catch (final NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        this.reAddSubWidgets();
    }

    protected void load() {
    	LitematicManager.getInstance().renderSyncmatic(placement);
        this.reAddSubWidgets();
    }

    protected void unload() {
    	LitematicManager.getInstance().unrenderSyncmatic(placement);
        this.reAddSubWidgets();
    }
}