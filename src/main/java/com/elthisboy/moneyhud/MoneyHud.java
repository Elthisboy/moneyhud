package com.elthisboy.moneyhud;

import com.elthisboy.moneyhud.command.MoneyHudServerCommand;
import com.elthisboy.moneyhud.network.MoneyHudPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoneyHud implements ModInitializer {

    public static final String MOD_ID = "moneyhud";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register the S2C payload type.
        PayloadTypeRegistry.playS2C().register(MoneyHudPayload.ID, MoneyHudPayload.CODEC);

        // Register server-side commands (command blocks / ops).
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                MoneyHudServerCommand.register(dispatcher));

        LOGGER.info("[MoneyHUD] Initialized.");
    }
}
