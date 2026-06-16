package com.cuboidclearer.game;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player state: the two selected corners and whether hammer mode is on.
 * Kept server-side and keyed by player UUID, so two players never share a selection.
 */
public final class Selection {

    private static final Map<UUID, BlockPos> firstCorner = new HashMap<>();
    private static final Map<UUID, BlockPos> secondCorner = new HashMap<>();
    private static final Set<UUID> hammerEnabled = new HashSet<>();
    private static final Set<UUID> hammerBusy = new HashSet<>();

    private Selection() {
    }

    public static void setFirstCorner(UUID player, BlockPos pos) {
        firstCorner.put(player, pos);
    }

    public static void setSecondCorner(UUID player, BlockPos pos) {
        secondCorner.put(player, pos);
    }

    public static boolean hasFirstCorner(UUID player) {
        return firstCorner.containsKey(player);
    }

    public static boolean bothCornersSet(UUID player) {
        return firstCorner.containsKey(player) && secondCorner.containsKey(player);
    }

    /** Builds the cuboid from the player's corners and clears the selection. */
    public static Cuboid take(UUID player) {
        Cuboid cuboid = Cuboid.between(firstCorner.get(player), secondCorner.get(player));
        clear(player);
        return cuboid;
    }

    public static void clear(UUID player) {
        firstCorner.remove(player);
        secondCorner.remove(player);
    }

    public static boolean hammerEnabled(UUID player) {
        return hammerEnabled.contains(player);
    }

    /** Flips hammer mode and returns the new state. */
    public static boolean toggleHammer(UUID player) {
        if (hammerEnabled.remove(player)) {
            return false;
        }
        hammerEnabled.add(player);
        return true;
    }

    public static boolean hammerBusy(UUID player) {
        return hammerBusy.contains(player);
    }

    public static void setHammerBusy(UUID player, boolean busy) {
        if (busy) {
            hammerBusy.add(player);
        } else {
            hammerBusy.remove(player);
        }
    }
}
