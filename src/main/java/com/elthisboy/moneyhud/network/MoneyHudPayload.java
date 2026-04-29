package com.elthisboy.moneyhud.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Custom S2C (server → client) payload that tells a client to change
 * the MoneyHUD state.
 *
 * action values:
 *   "on"     – enable HUD
 *   "off"    – disable HUD
 *   "tier:1" – switch to Tier 1
 *   "tier:2" – switch to Tier 2
 *   "tier:3" – switch to Tier 3
 *
 * Sent from MoneyHudServerCommand via ServerPlayNetworking.
 * Received in MoneyHudClient via ClientPlayNetworking.
 */
public record MoneyHudPayload(String action) implements CustomPayload {

    public static final CustomPayload.Id<MoneyHudPayload> ID =
            new CustomPayload.Id<>(Identifier.of("moneyhud", "control"));

    public static final PacketCodec<PacketByteBuf, MoneyHudPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.action()),
            buf -> new MoneyHudPayload(buf.readString())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
