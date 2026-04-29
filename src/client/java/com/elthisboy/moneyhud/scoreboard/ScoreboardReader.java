package com.elthisboy.moneyhud.scoreboard;

import com.elthisboy.moneyhud.config.MoneyHudConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.OptionalInt;

/**
 * Reads the player's money score from the scoreboard.
 *
 * WHY TWO CODE PATHS?
 * ───────────────────
 * Singleplayer
 *   The integrated server's Scoreboard and the client-side Scoreboard are
 *   separate objects. The client receives score data only via packets, and
 *   the integrated server only sends those packets when the objective is
 *   assigned to a display slot (sidebar, list, etc.).
 *   Solution: access client.getServer().getScoreboard() directly – no packet
 *   sync required, always up-to-date.
 *
 * Multiplayer
 *   client.getServer() returns null. We fall back to the client-side
 *   scoreboard (world.getScoreboard()), which IS kept in sync by the remote
 *   server for any score that changes while the player is connected.
 */
public class ScoreboardReader {

    private static final Logger LOGGER = LoggerFactory.getLogger("moneyhud");

    public static OptionalInt readMoneyScore() {
        MinecraftClient client = MinecraftClient.getInstance();

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) return OptionalInt.empty();

        String objectiveName = MoneyHudConfig.getInstance().scoreboardName;

        // ── Singleplayer: read the integrated server's scoreboard directly ──
        // This bypasses the packet-sync requirement entirely.
        MinecraftServer integratedServer = client.getServer();
        if (integratedServer != null) {
            return query(integratedServer.getScoreboard(), player, objectiveName);
        }

        // ── Multiplayer: use the client-side scoreboard ──
        return query(world.getScoreboard(), player, objectiveName);
    }

    /**
     * Looks up the player's score in the given scoreboard.
     * Tries two strategies in case the score is stored under a different
     * ScoreHolder type (entity vs. name-string, depending on how it was set).
     */
    private static OptionalInt query(Scoreboard scoreboard,
                                     ClientPlayerEntity player,
                                     String objectiveName) {
        try {
            ScoreboardObjective objective = scoreboard.getNullableObjective(objectiveName);
            if (objective == null) return OptionalInt.empty();

            // Strategy 1 – player entity as ScoreHolder
            ReadableScoreboardScore direct = scoreboard.getScore(player, objective);
            if (direct != null) return OptionalInt.of(direct.getScore());

            // Strategy 2 – name-only ScoreHolder
            // "/scoreboard players add @p money 100" stores the score under
            // the player's name string, not under the entity object.
            final String name = player.getNameForScoreboard();
            ScoreHolder nameHolder = new ScoreHolder() {
                @Override public String getNameForScoreboard() { return name; }
            };
            ReadableScoreboardScore byName = scoreboard.getScore(nameHolder, objective);
            if (byName != null) return OptionalInt.of(byName.getScore());

            return OptionalInt.empty();

        } catch (Exception e) {
            LOGGER.error("[MoneyHUD] Error querying scoreboard", e);
            return OptionalInt.empty();
        }
    }
}
