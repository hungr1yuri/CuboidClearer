package com.cuboidclearer;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

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
            if (world.isClient()) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!player.isSneaking()) return ActionResult.PASS;
            if (player.getMainHandStack().getItem() != Items.STICK) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            UUID uuid = player.getUuid();
            BlockPos clicked = hitResult.getBlockPos();

            if (!pos1Map.containsKey(uuid)) {
                pos1Map.put(uuid, clicked);
                player.sendMessage(Text.literal("\u00a7aPos1 set to " + clicked.toShortString() + " \u00a77(sneak+right-click again for Pos2)"), true);
                ((ServerWorld) world).spawnParticles(serverPlayer, new DustParticleEffect(0x00FF44, 1.8f),
                    true, false, clicked.getX() + 0.5, clicked.getY() + 0.5, clicked.getZ() + 0.5,
                    40, 0.5, 0.5, 0.5, 0.0);
            } else {
                pos2Map.put(uuid, clicked);
                player.sendMessage(Text.literal("\u00a7cPos2 set to " + clicked.toShortString() + " \u00a7e\u2014 run \u00a7f/cc clear\u00a7e or \u00a7f/cc fill"), true);
                ((ServerWorld) world).spawnParticles(serverPlayer, new DustParticleEffect(0xFF2222, 1.8f),
                    true, false, clicked.getX() + 0.5, clicked.getY() + 0.5, clicked.getZ() + 0.5,
                    40, 0.5, 0.5, 0.5, 0.0);
            }

            return ActionResult.SUCCESS;
        });

        // 3x3 hammer
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) return;
            UUID uuid = player.getUuid();
            if (!hammerEnabled.contains(uuid)) return;
            if (hammerBusy.contains(uuid)) return;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

            ItemStack tool = player.getMainHandStack();
            if (tool.isEmpty()) return;

            hammerBusy.add(uuid);
            Direction[] axes = getPerpendicularAxes(serverPlayer, pos);

            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    if (tool.isEmpty()) break;
                    BlockPos bp = pos.offset(axes[0], a).offset(axes[1], b);
                    var bState = world.getBlockState(bp);
                    if (bState.isAir()) continue;
                    if (bState.getBlock() == Blocks.BEDROCK) continue;

                    Block.dropStacks(bState, (ServerWorld) world, bp,
                        world.getBlockEntity(bp), player, tool);
                    world.breakBlock(bp, false, player);
                    // damage once per extra block broken — same as if player broke it normally
                    tool.damage(1, (ServerWorld) world, serverPlayer, item -> {});
                }
            }

            hammerBusy.remove(uuid);
        });

        // commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("cc")

                .then(CommandManager.literal("pos1")
                    .executes(ctx -> setPos(ctx, 1, null))
                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                        .executes(ctx -> setPos(ctx, 1, BlockPosArgumentType.getBlockPos(ctx, "pos")))))

                .then(CommandManager.literal("pos2")
                    .executes(ctx -> setPos(ctx, 2, null))
                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                        .executes(ctx -> setPos(ctx, 2, BlockPosArgumentType.getBlockPos(ctx, "pos")))))

                .then(CommandManager.literal("clear").executes(CuboidClearerMod::clear))
                .then(CommandManager.literal("fill").executes(CuboidClearerMod::fill))
                .then(CommandManager.literal("hammer").executes(CuboidClearerMod::hammer))
                .then(CommandManager.literal("info").executes(CuboidClearerMod::info))
                .then(CommandManager.literal("cancel").executes(CuboidClearerMod::cancel))
            );
        });
    }

    private static Direction[] getPerpendicularAxes(ServerPlayerEntity player, BlockPos brokenPos) {
        int playerY = player.getBlockPos().getY();
        int blockY = brokenPos.getY();
        if (blockY < playerY || blockY > playerY + 1) {
            return new Direction[]{Direction.EAST, Direction.NORTH};
        }
        return switch (player.getHorizontalFacing()) {
            case NORTH, SOUTH -> new Direction[]{Direction.EAST, Direction.UP};
            case EAST, WEST   -> new Direction[]{Direction.NORTH, Direction.UP};
            default           -> new Direction[]{Direction.EAST, Direction.NORTH};
        };
    }

    private static int hammer(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("Must be run by a player.")); return 0; }

        UUID uuid = player.getUuid();
        if (hammerEnabled.contains(uuid)) {
            hammerEnabled.remove(uuid);
            src.sendFeedback(() -> Text.literal("\u00a7cHammer mode OFF."), false);
        } else {
            hammerEnabled.add(uuid);
            src.sendFeedback(() -> Text.literal("\u00a7aHammer mode ON \u00a77\u2014 breaks 3x3, durability used per block."), false);
        }
        return 1;
    }

    private static int setPos(CommandContext<ServerCommandSource> ctx, int which, BlockPos explicit) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("Must be run by a player.")); return 0; }

        BlockPos pos = explicit != null ? explicit : player.getBlockPos();
        UUID uuid = player.getUuid();
        if (which == 1) pos1Map.put(uuid, pos);
        else            pos2Map.put(uuid, pos);

        src.sendFeedback(() -> Text.literal(
            "\u00a7aPos" + which + " set to " + pos.toShortString() +
            (bothSet(uuid) ? "  \u00a7e(both set \u2014 run \u00a7f/cc clear\u00a7e or \u00a7f/cc fill\u00a7e)" : "")), false);

        ServerWorld world = src.getWorld();
        world.spawnParticles(player, new DustParticleEffect(0x00FF44, 1.8f),
            true, false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            40, 0.5, 0.5, 0.5, 0.0);
        return 1;
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("Must be run by a player.")); return 0; }

        UUID uuid = player.getUuid();
        if (!bothSet(uuid)) {
            src.sendError(Text.literal("Set both positions first. Use /cc pos1 and /cc pos2, or sneak+right-click with a stick."));
            return 0;
        }

        ItemStack tool = player.getMainHandStack();
        if (tool.isEmpty() || tool.getMaxDamage() <= 0) {
            src.sendError(Text.literal("Hold a tool with durability in your main hand (pickaxe, axe, shovel etc)."));
            return 0;
        }

        BlockPos p1 = pos1Map.remove(uuid);
        BlockPos p2 = pos2Map.remove(uuid);

        int x1 = Math.min(p1.getX(), p2.getX()), x2 = Math.max(p1.getX(), p2.getX());
        int y1 = Math.min(p1.getY(), p2.getY()), y2 = Math.max(p1.getY(), p2.getY());
        int z1 = Math.min(p1.getZ(), p2.getZ()), z2 = Math.max(p1.getZ(), p2.getZ());
        long volume = (long)(x2-x1+1) * (y2-y1+1) * (z2-z1+1);

        if (volume > MAX_BLOCKS) {
            src.sendError(Text.literal("Too large! " + volume + " blocks (max " + MAX_BLOCKS + ")."));
            return 0;
        }

        ServerWorld world = src.getWorld();
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

                    Block.dropStacks(state, world, bp, world.getBlockEntity(bp), player, tool);
                    world.breakBlock(bp, false, player);
                    tool.damage(1, world, player, item -> {});
                    broken++;
                }
            }
        }

        final int result = broken;
        src.sendFeedback(() -> Text.literal("\u00a7aBroke \u00a7f" + result + "\u00a7a blocks."), false);
        return result;
    }

    private static int fill(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("Must be run by a player.")); return 0; }

        UUID uuid = player.getUuid();
        if (!bothSet(uuid)) {
            src.sendError(Text.literal("Set both positions first. Use /cc pos1 and /cc pos2, or sneak+right-click with a stick."));
            return 0;
        }

        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) {
            src.sendError(Text.literal("Hold the block you want to fill with in your main hand."));
            return 0;
        }

        Block fillBlock = Block.getBlockFromItem(held.getItem());
        if (fillBlock == Blocks.AIR) {
            src.sendError(Text.literal("That item isn't a placeable block."));
            return 0;
        }

        BlockState fillState = fillBlock.getDefaultState();
        BlockPos p1 = pos1Map.remove(uuid);
        BlockPos p2 = pos2Map.remove(uuid);

        int x1 = Math.min(p1.getX(), p2.getX()), x2 = Math.max(p1.getX(), p2.getX());
        int y1 = Math.min(p1.getY(), p2.getY()), y2 = Math.max(p1.getY(), p2.getY());
        int z1 = Math.min(p1.getZ(), p2.getZ()), z2 = Math.max(p1.getZ(), p2.getZ());
        long volume = (long)(x2-x1+1) * (y2-y1+1) * (z2-z1+1);

        if (volume > MAX_BLOCKS) {
            src.sendError(Text.literal("Too large! " + volume + " blocks (max " + MAX_BLOCKS + ")."));
            return 0;
        }

        int available = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == fillBlock.asItem()) available += stack.getCount();
        }

        if (available == 0) {
            src.sendError(Text.literal("You don't have any " + fillBlock.getName().getString() + " in your inventory."));
            return 0;
        }

        ServerWorld world = src.getWorld();
        drawOutline(world, player, x1, y1, z1, x2, y2, z2);

        int placed = 0;
        outer:
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    if (!world.getBlockState(bp).isAir()) continue;

                    boolean consumed = false;
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        ItemStack stack = player.getInventory().getStack(i);
                        if (stack.getItem() == fillBlock.asItem()) {
                            stack.decrement(1);
                            consumed = true;
                            break;
                        }
                    }
                    if (!consumed) break outer;

                    world.setBlockState(bp, fillState);
                    placed++;
                }
            }
        }

        final int result = placed;
        src.sendFeedback(() -> Text.literal("\u00a7aPlaced \u00a7f" + result + "\u00a7a blocks of "
            + fillBlock.getName().getString() + "."), false);
        return result;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) return 0;

        UUID uuid = player.getUuid();
        BlockPos p1 = pos1Map.get(uuid);
        BlockPos p2 = pos2Map.get(uuid);
        String s1 = p1 != null ? p1.toShortString() : "\u00a7cnot set";
        String s2 = p2 != null ? p2.toShortString() : "\u00a7cnot set";
        final String vol;
        if (p1 != null && p2 != null) {
            long v = (long)(Math.abs(p2.getX()-p1.getX())+1)
                   * (Math.abs(p2.getY()-p1.getY())+1)
                   * (Math.abs(p2.getZ()-p1.getZ())+1);
            vol = "  \u00a77(\u00a7f" + v + " \u00a77blocks)";
        } else {
            vol = "";
        }
        String hammerStatus = hammerEnabled.contains(uuid) ? "\u00a7aON" : "\u00a7cOFF";
        src.sendFeedback(() -> Text.literal(
            "\u00a76\u00a7lCuboidClearer\n" +
            "\u00a77Questions? Add \u00a7b@hungryuri \u00a77on Discord\n" +
            "\u00a77Commands: \u00a7fpos1, pos2, clear, fill, hammer, cancel\n" +
            "\u00a77Tip: \u00a7fSneak+right-click a block with a \u00a7astick\u00a7f to set positions\n" +
            "\u00a77============================\n" +
            "\u00a7aPos1: \u00a7f" + s1 + "\n" +
            "\u00a7cPos2: \u00a7f" + s2 + vol + "\n" +
            "\u00a7eHammer: " + hammerStatus), false);
        return 1;
    }

    private static int cancel(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) return 0;
        pos1Map.remove(player.getUuid());
        pos2Map.remove(player.getUuid());
        src.sendFeedback(() -> Text.literal("\u00a77Selection cleared."), false);
        return 1;
    }

    private static boolean bothSet(UUID uuid) {
        return pos1Map.containsKey(uuid) && pos2Map.containsKey(uuid);
    }

    private static void drawOutline(ServerWorld world, ServerPlayerEntity player,
                                    int x1, int y1, int z1, int x2, int y2, int z2) {
        DustParticleEffect red = new DustParticleEffect(0xFF2222, 1.0f);
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

    private static void dot(ServerWorld world, ServerPlayerEntity player,
                            DustParticleEffect dust, double x, double y, double z) {
        world.spawnParticles(player, dust, true, false, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}