package com.cuboidclearer.game;

import net.minecraft.core.BlockPos;

/**
 * An axis-aligned box between two corners, normalised so min &lt;= max on every axis.
 * Used by the clear and fill commands so the bounds and volume maths live in one place.
 */
public record Cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public static Cuboid between(BlockPos a, BlockPos b) {
        return new Cuboid(
            Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
            Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())
        );
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
}
