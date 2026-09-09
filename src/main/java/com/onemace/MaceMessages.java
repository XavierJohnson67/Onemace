package com.onemace;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class MaceMessages {

    private MaceMessages() {
    }

    public static void broadcast(MinecraftServer server, String message, ChatFormatting colour) {
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(message).withStyle(colour), false);
    }

    public static void tell(ServerPlayer player, String message, ChatFormatting colour) {
        player.sendSystemMessage(Component.literal(message).withStyle(colour));
    }

    public static void forged(MinecraftServer server, String playerName) {
        broadcast(server, playerName + " has forged the Mace! There can only be one - guard it well.", ChatFormatting.GOLD);
    }

    public static void claimed(MinecraftServer server, String playerName) {
        broadcast(server, playerName + " has claimed the fallen Mace!", ChatFormatting.GOLD);
    }

    public static void destroyed(MinecraftServer server) {
        broadcast(server, "The Mace has been destroyed! A new one may now be forged.", ChatFormatting.RED);
    }

    public static void confiscated(ServerPlayer player) {
        tell(player, "Only one Mace may exist on this server at a time - yours has been removed.", ChatFormatting.RED);
    }
}
