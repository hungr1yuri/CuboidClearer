package com.cuboidclearer;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CuboidClearerMod implements ModInitializer {

    private static final int MAX_BLOCKS = 5000;
    private static final Map<UUID, BlockPos> pos1Map = new HashMap<>();
    private static final Map<UUID, BlockPos> pos2Map = new HashMap<>();
    private static final Set<UUID> hammerEnabled = new HashSet<>();
    private static final Set<UUID> hammerBusy = new HashSet<>();

    @Override
    public void onInitialize() {

        // shift + right-click with stick to set pos1 then pos2
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;
            if (player.getMainHandItem().getItem() != Items.STICK) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            UUID uuid = player.getUUID();
            BlockPos clicked = hitResult.getBlockPos();

            if (!pos1Map.containsKey(uuid)) {
                pos1Map.put(uuid, clicked);
                serverPlayer.sendSystemMessage(Component.literal("§aPos1 set to " + clicked.toShortString() + " §7(sneak+right-click again for Pos2)"));
                ((ServerLevel) world).sendParticles(serverPlayer, new DustParticleOptions(0x00FF44, 1.8f),
                    true, false, clicked.getX() + 0.5, clicked.getY() + 0.5, clicked.getZ() + 0.5,
                    40, 0.5, 0.5, 0.5, 0.0);
            } else {
                pos2Map.put(uuid, clicked);
                serverPlayer.sendSystemMessage(Component.literal("§cPos2 set to " + clicked.toShortString() + " §e— run §f/cc clear§e or §f/cc fill"));
                ((ServerLevel) world).sendParticles(serverPlayer, new DustParticleOptions(0xFF2222, 1.8f),
                    true, false, clicked.getX() + 0.5, clicked.getY() + 0.5, clicked.getZ() + 0.5,
                    40, 0.5, 0.5, 0.5, 0.0);
            }

            return InteractionResult.SUCCESS;
        });

        // 3x3 hammer
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide()) return;
            UUID uuid = player.getUUID();
            if (!hammerEnabled.contains(uuid)) return;
            if (hammerBusy.contains(uuid)) return;
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            ItemStack tool = player.getMainHandItem();
            if (tool.isEmpty()) return;

            hammerBusy.add(uuid);
            Direction[] axes = getPerpendicularAxes(serverPlayer, pos);

            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    if (tool.isEmpty()) break;
                    BlockPos bp = pos.relative(axes[0], a).relative(axes[1], b);
                    var bState = world.getBlockState(bp);
                    if (bState.isAir()) continue;
                    if (bState.getBlock() == Blocks.BEDROCK) continue;

                    Block.dropResources(bState, world, bp, world.getBlockEntity(bp), player, tool);
                    world.destroyBlock(bp, false, player);
                    tool.hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
                }
            }

            hammerBusy.remove(uuid);
        });

        // join message
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            handler.player.sendSystemMessage(Component.literal(
                "§6§lCuboidClearer §r§7has been loaded. Type §f/cc info §7for help."))
        );

        // commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("cc")

                .then(Commands.literal("pos1")
                    .executes(ctx -> setPos(ctx, 1, null))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> setPos(ctx, 1, BlockPosArgument.getBlockPos(ctx, "pos")))))

                .then(Commands.literal("pos2")
                    .executes(ctx -> setPos(ctx, 2, null))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> setPos(ctx, 2, BlockPosArgument.getBlockPos(ctx, "pos")))))

                .then(Commands.literal("clear").executes(CuboidClearerMod::clear))
                .then(Commands.literal("fill").executes(CuboidClearerMod::fill))
                .then(Commands.literal("hammer").executes(CuboidClearerMod::hammer))
                .then(Commands.literal("info").executes(CuboidClearerMod::info))
                .then(Commands.literal("commands").executes(CuboidClearerMod::commandList))
                .then(Commands.literal("cancel").executes(CuboidClearerMod::cancel))
            );
        });
    }

    private static Direction[] getPerpendicularAxes(ServerPlayer player, BlockPos brokenPos) {
        int playerY = player.blockPosition().getY();
        int blockY = brokenPos.getY();
        if (blockY < playerY || blockY > playerY + 1) {
            return new Direction[]{Direction.EAST, Direction.NORTH};
        }
        return switch (player.getDirection()) {
            case NORTH, SOUTH -> new Direction[]{Direction.EAST, Direction.UP};
            case EAST, WEST   -> new Direction[]{Direction.NORTH, Direction.UP};
            default           -> new Direction[]{Direction.EAST, Direction.NORTH};
        };
    }

    private static int hammer(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) { src.sendFailure(Component.literal("Must be run by a player.")); return 0; }

        UUID uuid = player.getUUID();
        if (hammerEnabled.contains(uuid)) {
            hammerEnabled.remove(uuid);
            src.sendSuccess(() -> Component.literal("§cHammer mode OFF."), false);
        } else {
            hammerEnabled.add(uuid);
            src.sendSuccess(() -> Component.literal("§aHammer mode ON §7— breaks 3x3, durability used per block."), false);
        }
        return 1;
    }

    private static int setPos(CommandContext<CommandSourceStack> ctx, int which, BlockPos explicit) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) { src.sendFailure(Component.literal("Must be run by a player.")); return 0; }

        BlockPos pos = explicit != null ? explicit : player.blockPosition();
        UUID uuid = player.getUUID();
        if (which == 1) pos1Map.put(uuid, pos);
        else            pos2Map.put(uuid, pos);

        src.sendSuccess(() -> Component.literal(
            "§aPos" + which + " set to " + pos.toShortString() +
            (bothSet(uuid) ? "  §e(both set — run §f/cc clear§e or §f/cc fill§e)" : "")), false);

        ServerLevel world = src.getLevel();
        world.sendParticles(player, new DustParticleOptions(0x00FF44, 1.8f),
            true, false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            40, 0.5, 0.5, 0.5, 0.0);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) { src.sendFailure(Component.literal("Must be run by a player.")); return 0; }

        UUID uuid = player.getUUID();
        if (!bothSet(uuid)) {
            src.sendFailure(Component.literal("Set both positions first. Use /cc pos1 and /cc pos2, or sneak+right-click with a stick."));
            return 0;
        }

        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty() || tool.getMaxDamage() <= 0) {
            src.sendFailure(Component.literal("Hold a tool with durability in your main hand (pickaxe, axe, shovel etc)."));
            return 0;
        }

        BlockPos p1 = pos1Map.remove(uuid);
        BlockPos p2 = pos2Map.remove(uuid);

        int x1 = Math.min(p1.getX(), p2.getX()), x2 = Math.max(p1.getX(), p2.getX());
        int y1 = Math.min(p1.getY(), p2.getY()), y2 = Math.max(p1.getY(), p2.getY());
        int z1 = Math.min(p1.getZ(), p2.getZ()), z2 = Math.max(p1.getZ(), p2.getZ());
        long volume = (long)(x2-x1+1) * (y2-y1+1) * (z2-z1+1);

        if (volume > MAX_BLOCKS) {
            src.sendFailure(Component.literal("Too large! " + volume + " blocks (max " + MAX_BLOCKS + ")."));
            return 0;
        }

        ServerLevel world = src.getLevel();
        drawOutline(world, player, x1, y1, z1, x2, y2, z2);

        int broken = 0;
        outer:
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    if (tool.isEmpty()) break outer;
                    BlockPos bp = new BlockPos(x, y, z);
                    var state = world.getBlockState(bp);
                    if (state.isAir()) continue;
                    if (state.getBlock() == Blocks.BEDROCK) continue;

                    Block.dropResources(state, world, bp, world.getBlockEntity(bp), player, tool);
                    world.destroyBlock(bp, false, player);
                    tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    broken++;
                }
            }
        }

        final int result = broken;
        src.sendSuccess(() -> Component.literal("§aBroke §f" + result + "§a blocks."), false);
        return result;
    }

    private static int fill(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) { src.sendFailure(Component.literal("Must be run by a player.")); return 0; }

        UUID uuid = player.getUUID();
        if (!bothSet(uuid)) {
            src.sendFailure(Component.literal("Set both positions first. Use /cc pos1 and /cc pos2, or sneak+right-click with a stick."));
            return 0;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            src.sendFailure(Component.literal("Hold the block you want to fill with in your main hand."));
            return 0;
        }

        Block fillBlock = Block.byItem(held.getItem());
        if (fillBlock == Blocks.AIR) {
            src.sendFailure(Component.literal("That item isn't a placeable block."));
            return 0;
        }

        BlockState fillState = fillBlock.defaultBlockState();
        BlockPos p1 = pos1Map.remove(uuid);
        BlockPos p2 = pos2Map.remove(uuid);

        int x1 = Math.min(p1.getX(), p2.getX()), x2 = Math.max(p1.getX(), p2.getX());
        int y1 = Math.min(p1.getY(), p2.getY()), y2 = Math.max(p1.getY(), p2.getY());
        int z1 = Math.min(p1.getZ(), p2.getZ()), z2 = Math.max(p1.getZ(), p2.getZ());
        long volume = (long)(x2-x1+1) * (y2-y1+1) * (z2-z1+1);

        if (volume > MAX_BLOCKS) {
            src.sendFailure(Component.literal("Too large! " + volume + " blocks (max " + MAX_BLOCKS + ")."));
            return 0;
        }

        int available = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == fillBlock.asItem()) available += stack.getCount();
        }

        if (available == 0) {
            src.sendFailure(Component.literal("You don't have any " + fillBlock.getName().getString() + " in your inventory."));
            return 0;
        }

        ServerLevel world = src.getLevel();
        drawOutline(world, player, x1, y1, z1, x2, y2, z2);

        int placed = 0;
        outer:
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    if (!world.getBlockState(bp).isAir()) continue;

                    boolean consumed = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (stack.getItem() == fillBlock.asItem()) {
                            stack.shrink(1);
                            consumed = true;
                            break;
                        }
                    }
                    if (!consumed) break outer;

                    world.setBlockAndUpdate(bp, fillState);
                    placed++;
                }
            }
        }

        final int result = placed;
        src.sendSuccess(() -> Component.literal("§aPlaced §f" + result + "§a blocks of "
            + fillBlock.getName().getString() + "."), false);
        return result;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) return 0;

        UUID uuid = player.getUUID();
        String hammerStatus = hammerEnabled.contains(uuid) ? "§aON" : "§cOFF";
        src.sendSuccess(() -> Component.literal(
            "§6§lCuboidClearer\n" +
            "§7Questions? Add §b@hungryuri §7on Discord\n" +
            "§7Tip: §fShift§7+right-click a block with a §astick §7to set positions\n" +
            "§7Type §f/cc commands §7for all available commands\n" +
            "§7——————————————————\n" +
            "§eHammer: " + hammerStatus), false);
        return 1;
    }

    private static int cancel(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) return 0;
        pos1Map.remove(player.getUUID());
        pos2Map.remove(player.getUUID());
        src.sendSuccess(() -> Component.literal("§7Selection cleared."), false);
        return 1;
    }

    private static int commandList(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(
            "§6§lCuboidClearer §r§7— available commands:\n" +
            "§f/cc pos1 §8· §f/cc pos2 §7— select two corners\n" +
            "§f/cc clear §7— break all blocks in the selection\n" +
            "§f/cc fill §7— fill the selection with your held block\n" +
            "§f/cc hammer §7— toggle 3×3 mining mode\n" +
            "§f/cc cancel §7— clear your current selection\n" +
            "§f/cc info §7— show mod info & hammer status\n" +
            "§7Tip: §fShift§7+right-click with a §astick §7to set corners without commands"), false);
        return 1;
    }

    private static boolean bothSet(UUID uuid) {
        return pos1Map.containsKey(uuid) && pos2Map.containsKey(uuid);
    }

    private static void drawOutline(ServerLevel world, ServerPlayer player,
                                    int x1, int y1, int z1, int x2, int y2, int z2) {
        DustParticleOptions red = new DustParticleOptions(0xFF2222, 1.0f);
        for (int x = x1; x <= x2; x++) {
            dot(world, player, red, x+0.5, y1,     z1);
            dot(world, player, red, x+0.5, y2+1.0, z1);
            dot(world, player, red, x+0.5, y1,     z2+1.0);
            dot(world, player, red, x+0.5, y2+1.0, z2+1.0);
        }
        for (int y = y1; y <= y2; y++) {
            dot(world, player, red, x1,     y+0.5, z1);
            dot(world, player, red, x2+1.0, y+0.5, z1);
            dot(world, player, red, x1,     y+0.5, z2+1.0);
            dot(world, player, red, x2+1.0, y+0.5, z2+1.0);
        }
        for (int z = z1; z <= z2; z++) {
            dot(world, player, red, x1,     y1,     z+0.5);
            dot(world, player, red, x2+1.0, y1,     z+0.5);
            dot(world, player, red, x1,     y2+1.0, z+0.5);
            dot(world, player, red, x2+1.0, y2+1.0, z+0.5);
        }
    }

    private static void dot(ServerLevel world, ServerPlayer player,
                            DustParticleOptions dust, double x, double y, double z) {
        world.sendParticles(player, dust, true, false, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
