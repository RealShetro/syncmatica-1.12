package shetro.syncmatica.litematica.gui;

import shetro.syncmatica.Syncmatica;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.litematica.LitematicManager;
import litematica.gui.MainMenuScreen;
import malilib.gui.BaseListScreen;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.DataListWidget;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class SyncmaticaServerPlacementListScreen extends BaseListScreen<DataListWidget<ServerPlacement>> {
	protected final GenericButton mainMenuButton;
	protected final SyncmaticaServerPlacementInfoWidget syncmaticaServerPlacementInfoWidget;

	public SyncmaticaServerPlacementListScreen() {
		super(12, 30, 192, 56);

        this.syncmaticaServerPlacementInfoWidget = new SyncmaticaServerPlacementInfoWidget(170, 290);

		this.mainMenuButton = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.setTitle("syncmatica.gui.title.manage_server_placements", Syncmatica.VERSION);
        this.addPreScreenCloseListener(this::clearSchematicInfoCache);
	}

    @Override
    protected void reAddActiveWidgets() {
        super.reAddActiveWidgets();

        this.addWidget(this.syncmaticaServerPlacementInfoWidget);
        this.addWidget(this.mainMenuButton);
    }

    @Override
    protected void updateWidgetPositions() {
        super.updateWidgetPositions();

        this.syncmaticaServerPlacementInfoWidget.setHeight(this.getListHeight());
        this.syncmaticaServerPlacementInfoWidget.setRight(this.getRight() - 10);
        this.syncmaticaServerPlacementInfoWidget.setY(this.getListY());

        this.mainMenuButton.setRight(this.getRight() - 10);
        this.mainMenuButton.setBottom(this.getBottom() - 6);
    }

	@Override
	protected DataListWidget<ServerPlacement> createListWidget() {
		Supplier<List<ServerPlacement>> supplier = LitematicManager.getInstance().getActiveContext().getSyncmaticManager()::getAll;
        DataListWidget<ServerPlacement> listWidget = new DataListWidget<>(supplier, true);
        listWidget.addDefaultSearchBar();
        listWidget.setEntryFilter(SyncmaticaServerPlacementEntryWidget::serverPlacementSearchFilter);
        listWidget.setDataListEntryWidgetFactory(SyncmaticaServerPlacementEntryWidget::new);
        listWidget.setAllowSelection(true);
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);

        return listWidget;
	}

	public void onSelectionChange(@Nullable ServerPlacement entry) {
        this.syncmaticaServerPlacementInfoWidget.onSelectionChange(entry);
    }

    protected void clearSchematicInfoCache() {
        this.syncmaticaServerPlacementInfoWidget.clearCache();
    }
}