package com.cuboidclearer.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Small chat helpers. Colour codes are named here so the command code reads in words
 * instead of bare section symbols.
 */
public final class Messages {

    public static final String GREEN = "§a";
    public static final String RED = "§c";
    public static final String YELLOW = "§e";
    public static final String GRAY = "§7";
    public static final String WHITE = "§f";
    public static final String AQUA = "§b";
    public static final String GOLD = "§6";
    public static final String DARK_GRAY = "§8";
    public static final String BOLD = "§l";
    public static final String RESET = "§r";

    private Messages() {
    }

    public static Component text(String message) {
        return Component.literal(message);
    }

    /**
     * Returns the player who ran the command, or sends a friendly failure and returns
     * null when it came from the console or a command block.
     */
    public static ServerPlayer requirePlayer(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(text("Must be run by a player."));
        }
        return player;
    }
}
