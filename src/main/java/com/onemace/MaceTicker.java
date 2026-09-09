package com.onemace;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MaceTicker {

    /** How often (in ticks) we re-scan every player's inventory. 20 ticks = 1 second. */
    private static final int SCAN_INTERVAL_TICKS = 20;

    /** Radius searched for a dropped Mace around a player's last known position. */
    private static final double SEARCH_RADIUS = 12.0;

    /** Consecutive failed searches before we give up and call it destroyed. */
    private static final int MAX_MISSES = 3;

    private static int tickCounter = 0;

    private MaceTicker() {
    }

    public static void onServerTick(MinecraftServer server) {
        MaceState state = OneMaceMod.STATE;

        // Cheap check every single tick: is the mace we know is on the ground still there?
        // This is what actually notices lava/void/cactus destruction, so it runs every tick
        // rather than only on the slower inventory scan below.
        if (state.exists && state.groundEntityUuid != null) {
            pollGroundEntity(server, state);
        }

        tickCounter++;
        if (tickCounter < SCAN_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        scanInventories(server, state);
    }

    private static void scanInventories(MinecraftServer server, MaceState state) {
        List<ServerPlayer> holders = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (playerHasMace(player)) {
                holders.add(player);
            }
        }

        if (!state.exists) {
            if (holders.isEmpty()) {
                return;
            }
            ServerPlayer chosen = holders.get(0);
            for (int i = 1; i < holders.size(); i++) {
                confiscate(holders.get(i));
            }
            becomeHolder(server, state, chosen, true);
            return;
        }

        if (!holders.isEmpty()) {
            ServerPlayer chosen = null;
            for (ServerPlayer p : holders) {
                if (p.getUUID().equals(state.holderUuid)) {
                    chosen = p;
                    break;
                }
            }
            if (chosen == null) {
                chosen = holders.get(0);
            }
            for (ServerPlayer p : holders) {
                if (p != chosen) {
                    confiscate(p);
                }
            }

            boolean changedHands = !chosen.getUUID().equals(state.holderUuid);
            state.holderUuid = chosen.getUUID();
            state.holderName = chosen.getGameProfile().getName();
            updateLastKnown(state, chosen);
            state.groundEntityUuid = null;
            state.missCounter = 0;
            OneMaceMod.saveState();

            if (changedHands) {
                MaceMessages.claimed(server, state.holderName);
            }
            return;
        }

        // Not in anyone's inventory right now.
        if (state.groundEntityUuid == null) {
            tryLocateGroundEntity(server, state);
        }
        // If it IS tracked on the ground, the per-tick pollGroundEntity() above is handling it.
    }

    private static void becomeHolder(MinecraftServer server, MaceState state, ServerPlayer player, boolean freshlyForged) {
        state.exists = true;
        state.holderUuid = player.getUUID();
        state.holderName = player.getGameProfile().getName();
        updateLastKnown(state, player);
        state.groundEntityUuid = null;
        state.missCounter = 0;
        OneMaceMod.saveState();

        if (freshlyForged) {
            MaceMessages.forged(server, state.holderName);
        } else {
            MaceMessages.claimed(server, state.holderName);
        }
    }

    private static void updateLastKnown(MaceState state, ServerPlayer player) {
        state.lastKnownHolderUuid = player.getUUID();
        state.lastKnownDimension = player.level().dimension().location().toString();
        state.lastKnownX = player.getX();
        state.lastKnownY = player.getY();
        state.lastKnownZ = player.getZ();
    }

    private static void tryLocateGroundEntity(MinecraftServer server, MaceState state) {
        if (state.lastKnownHolderUuid == null) {
            // We have no idea where to even look - don't guess, just wait.
            return;
        }
        ServerLevel level = getLevelByKey(server, state.lastKnownDimension);
        if (level == null) {
            return;
        }

        AABB box = new AABB(
                state.lastKnownX, state.lastKnownY, state.lastKnownZ,
                state.lastKnownX, state.lastKnownY, state.lastKnownZ
        ).inflate(SEARCH_RADIUS);

        List<ItemEntity> found = level.getEntitiesOfClass(ItemEntity.class, box,
                e -> OneMaceUtil.isMace(e.getItem()));

        if (!found.isEmpty()) {
            ItemEntity entity = found.get(0);
            state.groundEntityUuid = entity.getUUID();
            state.groundDimension = state.lastKnownDimension;
            state.groundX = entity.getX();
            state.groundY = entity.getY();
            state.groundZ = entity.getZ();
            state.missCounter = 0;
            OneMaceMod.saveState();
            return;
        }

        state.missCounter++;
        if (state.missCounter >= MAX_MISSES) {
            declareDestroyed(server, state);
        } else {
            OneMaceMod.saveState();
        }
    }

    private static void pollGroundEntity(MinecraftServer server, MaceState state) {
        ServerLevel level = getLevelByKey(server, state.groundDimension);
        if (level == null) {
            return;
        }

        UUID uuid = state.groundEntityUuid;
        Entity entity = level.getEntity(uuid);
        if (entity != null && entity.isAlive()) {
            state.groundX = entity.getX();
            state.groundY = entity.getY();
            state.groundZ = entity.getZ();
            return;
        }

        BlockPos pos = BlockPos.containing(state.groundX, state.groundY, state.groundZ);
        boolean chunkLoaded = level.hasChunkAt(pos);
        if (!chunkLoaded) {
            // Can't tell right now - the chunk it was in has simply unloaded, not destroyed.
            // Leave the state as-is; we'll check again once it's back in range.
            return;
        }

        // Chunk is loaded, the entity we were tracking is gone, and the inventory scan
        // would already have cleared groundEntityUuid if a player had picked it up.
        // That only leaves destruction (lava, void, cactus, explosion, despawn timer, etc).
        declareDestroyed(server, state);
    }

    private static void declareDestroyed(MinecraftServer server, MaceState state) {
        state.clearToDestroyed();
        OneMaceMod.saveState();
        MaceMessages.destroyed(server);
    }

    private static ServerLevel getLevelByKey(MinecraftServer server, String dimensionKey) {
        if (dimensionKey == null || dimensionKey.isEmpty()) {
            return server.overworld();
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimensionKey)) {
                return level;
            }
        }
        return null;
    }

    private static boolean playerHasMace(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) {
            if (OneMaceUtil.isMace(stack)) {
                return true;
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (OneMaceUtil.isMace(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void confiscate(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        boolean removedAny = false;
        for (int i = 0; i < inventory.items.size(); i++) {
            if (OneMaceUtil.isMace(inventory.items.get(i))) {
                inventory.items.set(i, ItemStack.EMPTY);
                removedAny = true;
            }
        }
        for (int i = 0; i < inventory.offhand.size(); i++) {
            if (OneMaceUtil.isMace(inventory.offhand.get(i))) {
                inventory.offhand.set(i, ItemStack.EMPTY);
                removedAny = true;
            }
        }
        if (removedAny) {
            MaceMessages.confiscated(player);
        }
    }
}
