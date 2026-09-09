package com.onemace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class OneMaceMod implements ModInitializer {

    public static final String MOD_ID = "onemace";
    public static final Logger LOGGER = LoggerFactory.getLogger("OneMace");

    /** Server-wide state. Only ever touched on the server thread, so no locking. */
    public static MaceState STATE = new MaceState();

    private static Path stateFile;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            stateFile = server.getWorldPath(LevelResource.ROOT).resolve("onemace").resolve("state.properties");
            STATE = MaceState.load(stateFile);
            LOGGER.info("[OneMace] Loaded state - mace exists: {}", STATE.exists);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> saveState());

        ServerTickEvents.END_SERVER_TICK.register(MaceTicker::onServerTick);

        // Fast-path: as soon as a tracked holder dies, note where to look for the drop
        // instead of waiting up to a second for the next inventory scan to notice.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return;
            }
            if (STATE.exists && player.getUUID().equals(STATE.holderUuid)) {
                STATE.lastKnownHolderUuid = player.getUUID();
                STATE.lastKnownDimension = player.level().dimension().identifier().toString();
                STATE.lastKnownX = player.getX();
                STATE.lastKnownY = player.getY();
                STATE.lastKnownZ = player.getZ();
                STATE.holderUuid = null;
                saveState();
            }
        });

        LOGGER.info("[OneMace] Initialised.");
    }

    public static void saveState() {
        if (stateFile != null) {
            STATE.save(stateFile);
        }
    }
}
