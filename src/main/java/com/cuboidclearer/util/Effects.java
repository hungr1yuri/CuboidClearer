package com.cuboidclearer.util;

import com.cuboidclearer.game.Cuboid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Dust particles that show the player where their selection is. */
public final class Effects {

    private static final int FIRST_CORNER = 0x00FF44;
    private static final int SECOND_CORNER = 0xFF2222;

    private Effects() {
    }

    /** A small burst on a corner. The second corner is shown in red, the first in green. */
    public static void marker(ServerLevel world, ServerPlayer player, BlockPos pos, boolean second) {
        DustParticleOptions dust = new DustParticleOptions(second ? SECOND_CORNER : FIRST_CORNER, 1.8f);
        world.sendParticles(player, dust, true, false,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            40, 0.5, 0.5, 0.5, 0.0);
    }

    /** Traces the twelve edges of the selection so the player can see exactly what will change. */
    public static void outline(ServerLevel world, ServerPlayer player, Cuboid c) {
        DustParticleOptions red = new DustParticleOptions(SECOND_CORNER, 1.0f);
        double xLo = c.minX(), xHi = c.maxX() + 1.0;
        double yLo = c.minY(), yHi = c.maxY() + 1.0;
        double zLo = c.minZ(), zHi = c.maxZ() + 1.0;

        for (int x = c.minX(); x <= c.maxX(); x++) {
            dot(world, player, red, x + 0.5, yLo, zLo);
            dot(world, player, red, x + 0.5, yHi, zLo);
            dot(world, player, red, x + 0.5, yLo, zHi);
            dot(world, player, red, x + 0.5, yHi, zHi);
        }
        for (int y = c.minY(); y <= c.maxY(); y++) {
            dot(world, player, red, xLo, y + 0.5, zLo);
            dot(world, player, red, xHi, y + 0.5, zLo);
            dot(world, player, red, xLo, y + 0.5, zHi);
            dot(world, player, red, xHi, y + 0.5, zHi);
        }
        for (int z = c.minZ(); z <= c.maxZ(); z++) {
            dot(world, player, red, xLo, yLo, z + 0.5);
            dot(world, player, red, xHi, yLo, z + 0.5);
            dot(world, player, red, xLo, yHi, z + 0.5);
            dot(world, player, red, xHi, yHi, z + 0.5);
        }
    }

    private static void dot(ServerLevel world, ServerPlayer player,
                            DustParticleOptions dust, double x, double y, double z) {
        world.sendParticles(player, dust, true, false, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
