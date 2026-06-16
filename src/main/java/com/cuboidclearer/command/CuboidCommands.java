package com.cuboidclearer.command;

import com.cuboidclearer.game.Cuboid;
import com.cuboidclearer.game.Selection;
import com.cuboidclearer.util.Effects;
import com.cuboidclearer.util.Messages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

import static com.cuboidclearer.util.Messages.AQUA;
import static com.cuboidclearer.util.Messages.BOLD;
import static com.cuboidclearer.util.Messages.GOLD;
import static com.cuboidclearer.util.Messages.GRAY;
import static com.cuboidclearer.util.Messages.GREEN;
import static com.cuboidclearer.util.Messages.RED;
import static com.cuboidclearer.util.Messages.RESET;
import static com.cuboidclearer.util.Messages.WHITE;
import static com.cuboidclearer.util.Messages.YELLOW;

/** The {@code /cc} command tree and the actions behind it. */
public final class CuboidCommands {

    /** Hard cap on how many blocks one clear or fill may touch. */
    private static final int MAX_BLOCKS = 5000;

    private static final String NEED_SELECTION =
        "Set both positions first. Use /cc pos1 and /cc pos2, or sneak+right-click with a stick.";

    private CuboidCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cc")
            .then(Commands.literal("pos1")
                .executes(ctx -> setPos(ctx, 1, null))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(ctx -> setPos(ctx, 1, BlockPosArgument.getBlockPos(ctx, "pos")))))
            .then(Commands.literal("pos2")
                .executes(ctx -> setPos(ctx, 2, null))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(ctx -> setPos(ctx, 2, BlockPosArgument.getBlockPos(ctx, "pos")))))
            .then(Commands.literal("clear").executes(CuboidCommands::clear))
            .then(Commands.literal("fill").executes(CuboidCommands::fill))
            .then(Commands.literal("hammer").executes(CuboidCommands::hammer))
            .then(Commands.literal("info").executes(CuboidCommands::info))
            .then(Commands.literal("commands").executes(CuboidCommands::commandList))
            .then(Commands.literal("cancel").executes(CuboidCommands::cancel)));
    }

    private static int setPos(CommandContext<CommandSourceStack> ctx, int which, BlockPos explicit) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = Messages.requirePlayer(src);
        if (player == null) {
            return 0;
        }

        BlockPos pos = explicit != null ? explicit : player.blockPosition();
        UUID uuid = player.getUUID();
        if (which == 1) {
            Selection.setFirstCorner(uuid, pos);
        } else {
            Selection.setSecondCorner(uuid, pos);
        }

        String hint = Selection.bothCornersSet(uuid)
            ? "  " + YELLOW + "(both set, run " + WHITE + "/cc clear" + YELLOW + " or " + WHITE + "/cc fill" + YELLOW + ")"
            : "";
        src.sendSuccess(() -> Messages.text(GREEN + "Pos" + which + " set to " + pos.toShortString() + hint), false);

        Effects.marker(src.getLevel(), player, pos, false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = Messages.requirePlayer(src);
        if (player == null) {
            return 0;
        }

        UUID uuid = player.getUUID();
        if (!Selection.bothCornersSet(uuid)) {
            src.sendFailure(Messages.text(NEED_SELECTION));
            return 0;
        }

        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty() || tool.getMaxDamage() <= 0) {
            src.sendFailure(Messages.text("Hold a tool with durability in your main hand (pickaxe, axe, shovel etc)."));
            return 0;
        }

        Cuboid cuboid = Selection.take(uuid);
        if (cuboid.volume() > MAX_BLOCKS) {
            src.sendFailure(Messages.text("Too large! " + cuboid.volume() + " blocks (max " + MAX_BLOCKS + ")."));
            return 0;
        }

        ServerLevel world = src.getLevel();
        Effects.outline(world, player, cuboid);

        int broken = 0;
        outer:
        for (int x = cuboid.minX(); x <= cuboid.maxX(); x++) {
            for (int y = cuboid.minY(); y <= cuboid.maxY(); y++) {
                for (int z = cuboid.minZ(); z <= cuboid.maxZ(); z++) {
                    if (tool.isEmpty()) {
                        break outer;
                    }
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir() || state.getBlock() == Blocks.BEDROCK) {
                        continue;
                    }
                    Block.dropResources(state, world, pos, world.getBlockEntity(pos), player, tool);
                    world.destroyBlock(pos, false, player);
                    tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    broken++;
                }
            }
        }

        int result = broken;
        src.sendSuccess(() -> Messages.text(GREEN + "Broke " + WHITE + result + GREEN + " blocks."), false);
        return result;
    }

    private static int fill(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = Messages.requirePlayer(src);
        if (player == null) {
            return 0;
        }

        UUID uuid = player.getUUID();
        if (!Selection.bothCornersSet(uuid)) {
            src.sendFailure(Messages.text(NEED_SELECTION));
            return 0;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            src.sendFailure(Messages.text("Hold the block you want to fill with in your main hand."));
            return 0;
        }

        Block fillBlock = Block.byItem(held.getItem());
        if (fillBlock == Blocks.AIR) {
            src.sendFailure(Messages.text("That item isn't a placeable block."));
            return 0;
        }
        BlockState fillState = fillBlock.defaultBlockState();

        Cuboid cuboid = Selection.take(uuid);
        if (cuboid.volume() > MAX_BLOCKS) {
            src.sendFailure(Messages.text("Too large! " + cuboid.volume() + " blocks (max " + MAX_BLOCKS + ")."));
            return 0;
        }

        if (countInInventory(player, fillBlock) == 0) {
            src.sendFailure(Messages.text("You don't have any " + fillBlock.getName().getString() + " in your inventory."));
            return 0;
        }

        ServerLevel world = src.getLevel();
        Effects.outline(world, player, cuboid);

        int placed = 0;
        outer:
        for (int x = cuboid.minX(); x <= cuboid.maxX(); x++) {
            for (int y = cuboid.minY(); y <= cuboid.maxY(); y++) {
                for (int z = cuboid.minZ(); z <= cuboid.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!world.getBlockState(pos).isAir()) {
                        continue;
                    }
                    if (!consumeOne(player, fillBlock)) {
                        break outer;
                    }
                    world.setBlockAndUpdate(pos, fillState);
                    placed++;
                }
            }
        }

        int result = placed;
        src.sendSuccess(() -> Messages.text(GREEN + "Placed " + WHITE + result + GREEN + " blocks of "
            + fillBlock.getName().getString() + "."), false);
        return result;
    }

    private static int hammer(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = Messages.requirePlayer(src);
        if (player == null) {
            return 0;
        }

        if (Selection.toggleHammer(player.getUUID())) {
            src.sendSuccess(() -> Messages.text(GREEN + "Hammer mode ON " + GRAY + "(breaks 3x3, durability used per block)."), false);
        } else {
            src.sendSuccess(() -> Messages.text(RED + "Hammer mode OFF."), false);
        }
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            return 0;
        }

        String hammerStatus = Selection.hammerEnabled(player.getUUID()) ? GREEN + "ON" : RED + "OFF";
        src.sendSuccess(() -> Messages.text(
            GOLD + BOLD + "CuboidClearer\n"
                + GRAY + "Questions? Add " + AQUA + "@hungryuri " + GRAY + "on Discord\n"
                + GRAY + "Tip: " + WHITE + "Shift" + GRAY + "+right-click a block with a " + GREEN + "stick " + GRAY + "to set positions\n"
                + GRAY + "Type " + WHITE + "/cc commands " + GRAY + "for all available commands\n"
                + YELLOW + "Hammer: " + hammerStatus), false);
        return 1;
    }

    private static int cancel(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            return 0;
        }
        Selection.clear(player.getUUID());
        src.sendSuccess(() -> Messages.text(GRAY + "Selection cleared."), false);
        return 1;
    }

    private static int commandList(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Messages.text(
            GOLD + BOLD + "CuboidClearer " + RESET + GRAY + "commands:\n"
                + WHITE + "/cc pos1 " + GRAY + "and " + WHITE + "/cc pos2 " + GRAY + "select two corners\n"
                + WHITE + "/cc clear " + GRAY + "break all blocks in the selection\n"
                + WHITE + "/cc fill " + GRAY + "fill the selection with your held block\n"
                + WHITE + "/cc hammer " + GRAY + "toggle 3x3 mining mode\n"
                + WHITE + "/cc cancel " + GRAY + "clear your current selection\n"
                + WHITE + "/cc info " + GRAY + "show mod info and hammer status\n"
                + GRAY + "Tip: " + WHITE + "Shift" + GRAY + "+right-click with a " + GREEN + "stick " + GRAY + "to set corners without commands"), false);
        return 1;
    }

    private static int countInInventory(ServerPlayer player, Block block) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == block.asItem()) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean consumeOne(ServerPlayer player, Block block) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == block.asItem()) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
