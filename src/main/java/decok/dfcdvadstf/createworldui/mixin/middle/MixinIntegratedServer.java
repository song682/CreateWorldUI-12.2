package decok.dfcdvadstf.createworldui.mixin.middle;

import decok.dfcdvadstf.createworldui.api.DifficultyApplier;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <p>
 *     Mixin IntegratedServer to apply pending difficulty lock after world loading.
 *     After loadAllWorlds completes, worlds is fully initialized and can be accessed directly.
 * </p>
 */
@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer {

    private static final Logger createWorldUI$logger = LogManager.getLogger("MixinIntegratedServer");

    @Inject(method = "loadAllWorlds", at = @At("TAIL"))
    private void createWorldUI$onLoadAllWorlds(String saveName, String worldNameIn, long seed,
                                                WorldType type, String generatorOptions, CallbackInfo ci) {
        if (!DifficultyApplier.hasPendingDifficultyLock()) return;

        boolean difficultyLocked = DifficultyApplier.consumeDifficultyLocked();
        if (!difficultyLocked) return;

        createWorldUI$logger.info("Applying pending difficulty lock to world");

        IntegratedServer server = (IntegratedServer) (Object) this;
        if (server.worlds != null) {
            for (WorldServer worldServer : server.worlds) {
                if (worldServer != null) {
                    worldServer.getWorldInfo().setDifficultyLocked(true);
                }
            }
        }
    }
}
