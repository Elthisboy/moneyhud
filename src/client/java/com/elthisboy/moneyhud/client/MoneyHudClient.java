package com.elthisboy.moneyhud.client;

import com.elthisboy.moneyhud.command.MoneyHudCommand;
import com.elthisboy.moneyhud.config.MoneyHudConfig;
import com.elthisboy.moneyhud.hud.HudRenderer;
import com.elthisboy.moneyhud.network.MoneyHudPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoneyHudClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("moneyhud");

    /**
     * The shared renderer instance.
     * Public so the S2C packet receiver (registered below) can reach it
     * without a circular dependency.
     */
    public static HudRenderer RENDERER;

    @Override
    public void onInitializeClient() {
        // Load (or create) config/moneyhud.json
        MoneyHudConfig.getInstance();

        // Create the renderer; reads defaultHudTier + enabledByDefault from config
        RENDERER = new HudRenderer();

        // Register the per-frame HUD draw callback
        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
                RENDERER.render(drawContext));

        // Register client-side commands (/moneyhud on|off|tier|toggle|status)
        MoneyHudCommand.register(RENDERER);

        // ── Server → Client packet receiver ──────────────────────────────────
        // Handles packets sent by MoneyHudServerCommand (command blocks, ops).
        // Must run on the main thread, hence client.execute().
        ClientPlayNetworking.registerGlobalReceiver(MoneyHudPayload.ID,
            (payload, context) -> context.client().execute(() -> {
                switch (payload.action()) {
                    case "on"     -> RENDERER.setEnabled(true);
                    case "off"    -> RENDERER.setEnabled(false);
                    case "tier:1" -> RENDERER.setTier(1);
                    case "tier:2" -> RENDERER.setTier(2);
                    case "tier:3" -> RENDERER.setTier(3);
                }
            })
        );

        LOGGER.info("[MoneyHUD] Ready. Commands: /moneyhud on|off|tier <1-3>|toggle|status");
    }
}
