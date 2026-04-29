package com.elthisboy.moneyhud.command;

import com.elthisboy.moneyhud.network.MoneyHudPayload;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;

/**
 * Server-side /moneyhud commands.
 *
 * These run on the server (dedicated or integrated) so they can be executed
 * from command blocks, by operators, or via the server console.
 *
 * When executed, a MoneyHudPayload packet is sent to the targeted players;
 * the client receives it and updates its local HUD state.
 *
 * Syntax:
 *   /moneyhud on  [<targets>]   – show HUD
 *   /moneyhud off [<targets>]   – hide HUD
 *   /moneyhud tier 1|2|3 [<targets>]  – switch tier
 *
 * [<targets>] is optional – omit to apply to ALL online players.
 * Requires permission level 2 (operator / command block).
 */
public class MoneyHudServerCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("moneyhud")
                .requires(src -> src.hasPermissionLevel(2))

                // /moneyhud on [targets]
                .then(CommandManager.literal("on")
                    .executes(ctx ->
                        broadcast(ctx.getSource(), "on"))
                    .then(CommandManager.argument("targets", EntityArgumentType.players())
                        .executes(ctx ->
                            send(EntityArgumentType.getPlayers(ctx, "targets"), "on")))
                )

                // /moneyhud off [targets]
                .then(CommandManager.literal("off")
                    .executes(ctx ->
                        broadcast(ctx.getSource(), "off"))
                    .then(CommandManager.argument("targets", EntityArgumentType.players())
                        .executes(ctx ->
                            send(EntityArgumentType.getPlayers(ctx, "targets"), "off")))
                )

                // /moneyhud tier <1|2|3> [targets]
                .then(CommandManager.literal("tier")
                    .then(CommandManager.literal("1")
                        .executes(ctx ->
                            broadcast(ctx.getSource(), "tier:1"))
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                            .executes(ctx ->
                                send(EntityArgumentType.getPlayers(ctx, "targets"), "tier:1")))
                    )
                    .then(CommandManager.literal("2")
                        .executes(ctx ->
                            broadcast(ctx.getSource(), "tier:2"))
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                            .executes(ctx ->
                                send(EntityArgumentType.getPlayers(ctx, "targets"), "tier:2")))
                    )
                    .then(CommandManager.literal("3")
                        .executes(ctx ->
                            broadcast(ctx.getSource(), "tier:3"))
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                            .executes(ctx ->
                                send(EntityArgumentType.getPlayers(ctx, "targets"), "tier:3")))
                    )
                )
        );
    }

    /** Sends the action to every online player. */
    private static int broadcast(ServerCommandSource source, String action) {
        Collection<ServerPlayerEntity> all = source.getServer()
                .getPlayerManager().getPlayerList();
        return send(all, action);
    }

    /** Sends the action packet to each player in the collection. */
    private static int send(Collection<ServerPlayerEntity> targets, String action) {
        MoneyHudPayload payload = new MoneyHudPayload(action);
        for (ServerPlayerEntity player : targets) {
            ServerPlayNetworking.send(player, payload);
        }
        return targets.size();
    }
}
