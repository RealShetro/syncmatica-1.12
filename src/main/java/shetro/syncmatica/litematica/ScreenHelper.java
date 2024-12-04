package shetro.syncmatica.litematica;

import shetro.syncmatica.Context;
import shetro.syncmatica.ServerPlacement;
import malilib.gui.BaseScreen;
import malilib.overlay.message.MessageDispatcher;

import java.util.function.Consumer;

public class ScreenHelper {
    private static ScreenHelper instance;

    private Consumer<ServerPlacement> updateListener;
    private BaseScreen currentGui = null;
    private Context context;

    public static void ifPresent(final Consumer<ScreenHelper> callable) {
        if (instance != null) {
            callable.accept(instance);
        }
    }

    public static void init() {
        if (instance != null) {
            instance.detach();
        }
        instance = new ScreenHelper();
    }

    public static void close() {
        if (instance != null) {
            instance.detach();
        }
        instance = null;
    }

    private ScreenHelper() {
    }

    public void setActiveContext(final Context con) {
        detach();
        context = con;
        attach();
        updateCurrentScreen();
    }

    public void setCurrentGui(final BaseScreen gui) {
        currentGui = gui;
    }

    public void addMessage(final int messageTypeColor, final String messageKey, final Object... args) {
        new MessageDispatcher(messageTypeColor).translate(messageKey, args);
    }

    private void updateCurrentScreen() {
        if (currentGui != null) {
            currentGui.initGui();
        }
    }

    private void attach() {
        updateListener = p -> updateCurrentScreen();
        context.getSyncmaticManager().addServerPlacementConsumer(updateListener);
    }

    private void detach() {
        if (context != null) {
            context.getSyncmaticManager().removeServerPlacementConsumer(updateListener);
        }
    }
}