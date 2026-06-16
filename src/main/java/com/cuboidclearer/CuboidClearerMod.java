package com.cuboidclearer;

import com.cuboidclearer.command.CuboidCommands;
import com.cuboidclearer.game.Hammer;
import com.cuboidclearer.game.Selection;
import com.cuboidclearer.util.Effects;
import com.cuboidclearer.util.Messages;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

import java.util.UUID;

import static com.cuboidclearer.util.Messages.BOLD;
import static com.cuboidclearer.util.Messages.GOLD;
import static com.cuboidclearer.util.Messages.GRAY;
import static com.cuboidclearer.util.Messages.GREEN;
import static com.cuboidclearer.util.Messages.RED;
import static com.cuboidclearer.util.Messages.RESET;
import static com.cuboidclearer.util.Messages.WHITE;
import static com.cuboidclearer.util.Messages.YELLOW;

/**
 * Fabric entry point. All it does is wire the loader's events to the shared
 * command, selection and hammer logic.
 */
public class CuboidClearerMod implements ModInitializer {

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register(this::onStickRightClick);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                runHammer((ServerLevel) world, serverPlayer, pos);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            handler.player.sendSystemMessage(Messages.text(
                GOLD + BOLD + "CuboidClearer " + RESET + GRAY + "loaded. Type " + WHITE + "/cc info " + GRAY + "for help.")));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            CuboidCommands.register(dispatcher));
    }

    private InteractionResult onStickRightClick(net.minecraft.world.entity.player.Player player,
                                                net.minecraft.world.level.Level world,
                                                InteractionHand hand,
                                                net.minecraft.world.phys.BlockHitResult hitResult) {
        if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown() || player.getMainHandItem().getItem() != Items.STICK) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        UUID uuid = serverPlayer.getUUID();
        BlockPos clicked = hitResult.getBlockPos();
        ServerLevel level = (ServerLevel) world;

        if (!Selection.hasFirstCorner(uuid)) {
            Selection.setFirstCorner(uuid, clicked);
            serverPlayer.sendSystemMessage(Messages.text(
                GREEN + "Pos1 set to " + clicked.toShortString() + " " + GRAY + "(sneak+right-click again for Pos2)"));
            Effects.marker(level, serverPlayer, clicked, false);
        } else {
            Selection.setSecondCorner(uuid, clicked);
            serverPlayer.sendSystemMessage(Messages.text(
                RED + "Pos2 set to " + clicked.toShortString() + " " + YELLOW + "(run " + WHITE + "/cc clear" + YELLOW + " or " + WHITE + "/cc fill" + YELLOW + ")"));
            Effects.marker(level, serverPlayer, clicked, true);
        }
        return InteractionResult.SUCCESS;
    }

    private void runHammer(ServerLevel world, ServerPlayer player, BlockPos pos) {
        UUID uuid = player.getUUID();
        if (!Selection.hammerEnabled(uuid) || Selection.hammerBusy(uuid)) {
            return;
        }
        Selection.setHammerBusy(uuid, true);
        Hammer.breakAround(world, player, pos);
        Selection.setHammerBusy(uuid, false);
    }
}
