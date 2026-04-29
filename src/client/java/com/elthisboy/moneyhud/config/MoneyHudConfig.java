package com.elthisboy.moneyhud.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Config for MoneyHUD. Loaded from/saved to config/moneyhud.json.
 * All fields are public so Gson can serialize/deserialize them directly.
 */
public class MoneyHudConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("moneyhud");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("moneyhud.json");

    // ── Scoreboard ────────────────────────────────────────────────
    /** Name of the scoreboard objective that holds the money value. */
    public String scoreboardName = "money";

    // ── Display labels ────────────────────────────────────────────
    /** Currency symbol shown in the HUD (e.g. "$"). */
    public String currencySymbol = "$";

    // ── Position ─────────────────────────────────────────────────
    /** One of: top_left, top_right, bottom_left, bottom_right */
    public String position = "top_right";
    /** Horizontal offset from the chosen edge (pixels). */
    public int xOffset = 6;
    /** Vertical offset from the chosen edge (pixels). */
    public int yOffset = 20;
    /** Overall scale multiplier for the HUD (1.0 = normal). */
    public float scale = 1.0f;

    // ── Background ───────────────────────────────────────────────
    public boolean backgroundEnabled = true;
    /** 0.0 = fully transparent, 1.0 = fully opaque. */
    public float backgroundOpacity = 0.75f;

    // ── Icon ─────────────────────────────────────────────────────
    public boolean iconEnabled = true;

    // ── Colors (hex strings, with or without leading #) ──────────
    /** Main accent color (used for borders, symbol, highlights). */
    public String accentColor  = "#FFD700";   // gold
    /** Label text color. */
    public String labelColor   = "#AAAAAA";   // light grey
    /** Numeric value text color. */
    public String valueColor   = "#FFFFFF";   // white
    /** Currency symbol color. */
    public String symbolColor  = "#FFD700";   // gold

    // ── Animation ────────────────────────────────────────────────
    /** Enables the pop/flash animation when money value changes. */
    public boolean animationEnabled = true;

    // ── HUD state defaults ────────────────────────────────────────
    /** Which tier (1–3) to start with. */
    public int defaultHudTier = 1;
    /** Whether the HUD is visible when the game starts. */
    public boolean enabledByDefault = true;

    // ─────────────────────────────────────────────────────────────
    // Singleton management
    // ─────────────────────────────────────────────────────────────

    private static MoneyHudConfig instance;

    /** Returns the loaded config instance, creating it if needed. */
    public static MoneyHudConfig getInstance() {
        if (instance == null) instance = load();
        return instance;
    }

    /** Loads config from disk, or creates a default one if missing. */
    public static MoneyHudConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                MoneyHudConfig cfg = GSON.fromJson(reader, MoneyHudConfig.class);
                if (cfg != null) {
                    instance = cfg;
                    return cfg;
                }
            } catch (IOException e) {
                LOGGER.error("[MoneyHUD] Failed to read config – using defaults.", e);
            }
        }
        // First launch: write defaults to disk
        instance = new MoneyHudConfig();
        instance.save();
        LOGGER.info("[MoneyHUD] Default config written to {}", CONFIG_PATH);
        return instance;
    }

    /** Persists the current instance to disk. */
    public void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.error("[MoneyHUD] Failed to save config.", e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Color helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Parses a hex color string such as "#FFD700" or "FFD700" into an ARGB int.
     * If no alpha is specified, alpha is set to 0xFF (fully opaque).
     */
    public static int parseColor(String hex) {
        try {
            String s = hex.startsWith("#") ? hex.substring(1) : hex;
            if (s.length() == 6) s = "FF" + s;  // add full alpha
            return (int) Long.parseLong(s, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF; // fallback: opaque white
        }
    }
}
