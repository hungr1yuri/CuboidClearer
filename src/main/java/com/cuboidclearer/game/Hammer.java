package com.cuboidclearer.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 3x3 mining: when a player breaks a block with hammer mode on, the eight blocks
 * around it (on the face the player is looking at) break too, each costing durability.
 */
public final class Hammer {

    private Hammer() {
    }

    public static void breakAround(ServerLevel world, ServerPlayer player, BlockPos center) {
        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty()) {
            return;
        }

        Direction[] axes = perpendicularAxes(player, center);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                if (tool.isEmpty()) {
                    break;
                }
                BlockPos pos = center.relative(axes[0], a).relative(axes[1], b);
                BlockState state = world.getBlockState(pos);
                if (state.isAir() || state.getBlock() == Blocks.BEDROCK) {
                    continue;
                }
                Block.dropResources(state, world, pos, world.getBlockEntity(pos), player, tool);
                world.destroyBlock(pos, false, player);
                tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
        }
    }

    /** The two axes that span the 3x3 plane facing the player, picked from where they're looking. */
    private static Direction[] perpendicularAxes(ServerPlayer player, BlockPos brokenPos) {
        int playerY = player.blockPosition().getY();
        int blockY = brokenPos.getY();
        if (blockY < playerY || blockY > playerY + 1) {
            return new Direction[]{Direction.EAST, Direction.NORTH};
        }
        return switch (player.getDirection()) {
            case NORTH, SOUTH -> new Direction[]{Direction.EAST, Direction.UP};
            case EAST, WEST -> new Direction[]{Direction.NORTH, Direction.UP};
            default -> new Direction[]{Direction.EAST, Direction.NORTH};
        };
    }
}
