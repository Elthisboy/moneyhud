package com.elthisboy.moneyhud.command;

import com.elthisboy.moneyhud.hud.HudRenderer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

/**
 * Registers all /moneyhud client-side commands.
 *
 * All feedback messages use Text.translatable() so they respect the player's
 * language setting (en_us, es_es, de_de, etc.).
 *
 * Command tree:
 *   /moneyhud on
 *   /moneyhud off
 *   /moneyhud toggle
 *   /moneyhud tier 1 | 2 | 3
 *   /moneyhud status
 */
public class MoneyHudCommand {

    public static void register(HudRenderer renderer) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                ClientCommandManager.literal("moneyhud")

                    // /moneyhud on
                    .then(ClientCommandManager.literal("on").executes(ctx -> {
                        renderer.setEnabled(true);
                        ctx.getSource().sendFeedback(
                                Text.translatable("moneyhud.command.on"));
                        return 1;
                    }))

                    // /moneyhud off
                    .then(ClientCommandManager.literal("off").executes(ctx -> {
                        renderer.setEnabled(false);
                        ctx.getSource().sendFeedback(
                                Text.translatable("moneyhud.command.off"));
                        return 1;
                    }))

                    // /moneyhud toggle
                    .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                        boolean now = !renderer.isEnabled();
                        renderer.setEnabled(now);
                        ctx.getSource().sendFeedback(
                                Text.translatable(now
                                        ? "moneyhud.command.toggle.on"
                                        : "moneyhud.command.toggle.off"));
                        return 1;
                    }))

                    // /moneyhud tier <1|2|3>
                    .then(ClientCommandManager.literal("tier")
                        .then(ClientCommandManager.literal("1").executes(ctx -> {
                            renderer.setTier(1);
                            ctx.getSource().sendFeedback(
                                    Text.translatable("moneyhud.command.tier.1"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("2").executes(ctx -> {
                            renderer.setTier(2);
                            ctx.getSource().sendFeedback(
                                    Text.translatable("moneyhud.command.tier.2"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("3").executes(ctx -> {
                            renderer.setTier(3);
                            ctx.getSource().sendFeedback(
                                    Text.translatable("moneyhud.command.tier.3"));
                            return 1;
                        }))
                    )

                    // /moneyhud status
                    .then(ClientCommandManager.literal("status").executes(ctx -> {
                        String enabledKey = renderer.isEnabled()
                                ? "moneyhud.command.status.enabled"
                                : "moneyhud.command.status.disabled";
                        String tierKey = switch (renderer.getTier()) {
                            case 2  -> "moneyhud.command.status.tier.2";
                            case 3  -> "moneyhud.command.status.tier.3";
                            default -> "moneyhud.command.status.tier.1";
                        };
                        ctx.getSource().sendFeedback(
                                Text.translatable("moneyhud.command.status",
                                        Text.translatable(enabledKey),
                                        Text.translatable(tierKey)));
                        return 1;
                    }))
            )
        );
    }
}
