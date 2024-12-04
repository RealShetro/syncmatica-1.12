package shetro.syncmatica.mixin;

import shetro.syncmatica.FileStorage;
import shetro.syncmatica.SyncmaticManager;
import shetro.syncmatica.Syncmatica;
import shetro.syncmatica.communication.ServerCommunicationManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Inject(method = "initialWorldChunkLoad", at = @At("HEAD"))
    public void initSyncmatica(final CallbackInfo ci) {
        final MinecraftServer minecraftServer = (MinecraftServer) (Object) this;
        Syncmatica.initServer(
                new ServerCommunicationManager(),
                new FileStorage(),
                new SyncmaticManager(),
                !minecraftServer.isDedicatedServer(),
                minecraftServer.getActiveAnvilConverter().getFile(minecraftServer.getFolderName(), ".")
        ).startup();
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    public void shutdownSyncmatica(final CallbackInfo ci) {
        Syncmatica.shutdown();
    }
}