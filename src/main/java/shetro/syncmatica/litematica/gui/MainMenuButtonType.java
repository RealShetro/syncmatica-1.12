package shetro.syncmatica.litematica.gui;

public enum MainMenuButtonType {

    VIEW_SYNCMATICS("syncmatica.gui.button.view_syncmatics"),
    MATERIAL_GATHERINGS("syncmatica.gui.button.material_gatherings");

    private final String labelKey;

    MainMenuButtonType(final String labelKey) {
        this.labelKey = labelKey;
    }
}