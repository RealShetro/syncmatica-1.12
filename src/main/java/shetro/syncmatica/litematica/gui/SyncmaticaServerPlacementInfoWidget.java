package shetro.syncmatica.litematica.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

import shetro.syncmatica.ServerPlacement;
import malilib.gui.widget.ContainerWidget;
import malilib.gui.widget.LabelWidget;
import malilib.render.text.StyledTextLine;
import net.minecraft.util.math.BlockPos;

public class SyncmaticaServerPlacementInfoWidget extends ContainerWidget {
	protected final LabelWidget infoTextLabel;
    public ServerPlacement placement;

    public SyncmaticaServerPlacementInfoWidget(int width, int height) {
        super(width, height);

        this.infoTextLabel = new LabelWidget();
        this.infoTextLabel.setLineHeight(12);

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xC0000000);
        this.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFC0C0C0);
    }

    @Override
    public void reAddSubWidgets() {
        super.reAddSubWidgets();

        if (this.placement == null) {
            return;
        }

        this.addWidget(this.infoTextLabel);
    }

    @Override
    public void updateSubWidgetPositions() {
        if (this.placement == null) {
            return;
        }

        int x = this.getX() + 4;
        int y = this.getY() + 4;

        this.infoTextLabel.setPosition(x, y);
    }

    //Idk if this is necessary for sure
    public void clearCache() {
        this.placement = null;
    }

    public void onSelectionChange(@Nullable ServerPlacement entry) {
        if (entry != null) {
            this.placement = entry;
        } else {
            this.placement = null;
        }

        this.onPostSelectionChange();
    }

    protected void onPostSelectionChange() {
        this.updateWidgetState();
        this.updateSubWidgetPositions();
        this.reAddSubWidgets();
    }

    @Override
    public void updateWidgetState() {
        this.updateInfoLabelText();
    }

    protected void updateInfoLabelText() {
        if (this.placement == null) {
            this.infoTextLabel.setLines(Collections.emptyList());
            return;
        }

        List<StyledTextLine> lines = new ArrayList<>();

        StyledTextLine.translate(lines, "syncmatica.gui.label.placement_info.file_name", this.placement.getName());

        StyledTextLine.translate(lines, "syncmatica.gui.label.placement_info.dimension_id", this.placement.getDimension());

        final BlockPos origin = this.placement.getPosition();
        final String tmp = String.format("%d %d %d", origin.getX(), origin.getY(), origin.getZ());
        StyledTextLine.translate(lines, "syncmatica.gui.label.placement_info.position", tmp);

        StyledTextLine.translate(lines, "syncmatica.gui.label.placement_info.owner", this.placement.getOwner().getName());

        StyledTextLine.translate(lines, "syncmatica.gui.label.placement_info.last_modified", this.placement.getLastModifiedBy().getName());

        this.infoTextLabel.setLines(lines);
    }
}