package com.elthisboy.moneyhud.hud;

import com.elthisboy.moneyhud.config.MoneyHudConfig;
import com.elthisboy.moneyhud.scoreboard.ScoreboardReader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;

import java.text.NumberFormat;
import java.util.OptionalInt;

/**
 * Renders the MoneyHUD overlay every frame via HudRenderCallback.
 *
 * ── Tier 1 "Moneda"  ── compact badge + label strip, silver/gray feel
 * ── Tier 2 "Cartera" ── split card: icon panel left / value panel right, gold
 * ── Tier 3 "Bóveda"  ── elite multi-zone panel, gold + purple + teal
 *
 * Color convention: all ints are ARGB (0xAARRGGBB).
 */
public class HudRenderer {

    // ─────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────

    private boolean enabled;
    private int     tier;

    private final HudAnimator animator = new HudAnimator();

    /** Formats numbers with locale thousands separators: 1300 → "1,300". */
    private static final NumberFormat NUM_FMT = NumberFormat.getNumberInstance();

    /** Wall-clock timestamp of the previous frame, for delta-time animation. */
    private long lastRenderMs = System.currentTimeMillis();

    // ─────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────

    public HudRenderer() {
        MoneyHudConfig cfg = MoneyHudConfig.getInstance();
        this.enabled = cfg.enabledByDefault;
        this.tier    = MathHelper.clamp(cfg.defaultHudTier, 1, 3);
    }

    // ─────────────────────────────────────────────────────────────
    // Public API (called by commands)
    // ─────────────────────────────────────────────────────────────

    public void setEnabled(boolean v) { this.enabled = v; }
    public boolean isEnabled()        { return enabled; }

    public void setTier(int t) { this.tier = MathHelper.clamp(t, 1, 3); }
    public int  getTier()      { return tier; }

    // ─────────────────────────────────────────────────────────────
    // Main render entry point
    // ─────────────────────────────────────────────────────────────

    public void render(DrawContext ctx) {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Show during chat (player may want to see balance while typing),
        // hide for everything else (inventory, menus, pause screen).
        Screen screen = client.currentScreen;
        if (screen != null && !(screen instanceof ChatScreen)) return;

        // ── Delta time (wall clock) ──────────────────────────────
        long now = System.currentTimeMillis();
        float deltaSeconds = (now - lastRenderMs) / 1000f;
        lastRenderMs = now;

        // ── Score read ───────────────────────────────────────────
        OptionalInt moneyOpt = ScoreboardReader.readMoneyScore();
        String valueText = moneyOpt.isPresent()
                ? NUM_FMT.format(moneyOpt.getAsInt())
                : "---";

        MoneyHudConfig cfg = MoneyHudConfig.getInstance();
        if (moneyOpt.isPresent() && cfg.animationEnabled) {
            animator.tick(moneyOpt.getAsInt(), deltaSeconds);
        }

        // ── Scale + layout ───────────────────────────────────────
        TextRenderer tr    = client.textRenderer;
        float        scale = Math.max(0.1f, cfg.scale);

        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1f);

        int scaledW = (int) (client.getWindow().getScaledWidth()  / scale);
        int scaledH = (int) (client.getWindow().getScaledHeight() / scale);

        int panelW, panelH;
        switch (tier) {
            case 2  -> { panelW = computeTier2Width(tr, cfg, valueText); panelH = 44; }
            case 3  -> { panelW = computeTier3Width(tr, cfg, valueText); panelH = 64; }
            default -> { panelW = computeTier1Width(tr, cfg, valueText); panelH = 26; }
        }

        int panelX = switch (cfg.position) {
            case "top_right", "bottom_right" -> scaledW - panelW - cfg.xOffset;
            default -> cfg.xOffset;
        };
        int panelY = switch (cfg.position) {
            case "bottom_left", "bottom_right" -> scaledH - panelH - cfg.yOffset;
            default -> cfg.yOffset;
        };

        switch (tier) {
            case 2  -> renderTier2(ctx, tr, cfg, panelX, panelY, panelW, panelH, valueText);
            case 3  -> renderTier3(ctx, tr, cfg, panelX, panelY, panelW, panelH, valueText);
            default -> renderTier1(ctx, tr, cfg, panelX, panelY, panelW, panelH, valueText);
        }

        ctx.getMatrices().pop();
    }

    // ═════════════════════════════════════════════════════════════
    // TIER 1  "Moneda"  ─  Badge strip  (h = 26 px)
    //
    //  ████████████████████████████████████████████
    //  █ [████$████] │ DINERO:        1,300       █  ← 22 px content
    //  ████████████████████████████████████████████
    //   ↑ 2px accent   ↑ badge zone   ↑ separator
    //
    //  • accent-tinted badge square on the left with $ centred
    //  • thin vertical separator
    //  • label + value on a single row
    //  • 2 px top bar (accent), 1 px bottom edge (dim)
    //  • right-edge hair-line border
    // ═════════════════════════════════════════════════════════════

    private void renderTier1(DrawContext ctx, TextRenderer tr, MoneyHudConfig cfg,
                              int x, int y, int w, int h, String valueText) {

        // ── Tier 1 palette: gray ──────────────────────────────────
        final int accent = 0xFF888888;   // medium gray – borders, badge, top bar
        final int labelC = 0xFF9A9A9A;   // slightly lighter gray – label text
        final int valueC = 0xFFD4D4D4;   // near-white silver – value text

        // Badge zone width: padding + symbol + padding  (0 if icon disabled)
        int badgeW = cfg.iconEnabled
                ? Math.max(18, 5 + tr.getWidth(cfg.currencySymbol) + 5)
                : 0;

        // ── Background ───────────────────────────────────────────
        if (cfg.backgroundEnabled) {
            int bgA = alphaFromOpacity(cfg.backgroundOpacity);
            ctx.fill(x, y, x + w, y + h, argb(bgA, 0x0E, 0x0E, 0x0E));
        }

        // Badge zone: semi-transparent accent tint
        if (cfg.iconEnabled) {
            ctx.fill(x, y + 2, x + badgeW, y + h - 1,
                    accentTint(accent, 0x38));
        }

        // ── Borders ──────────────────────────────────────────────
        // Top bar: 2 px, full accent
        ctx.fill(x, y, x + w, y + 2, accent);
        // Bottom edge: 1 px dim
        ctx.fill(x, y + h - 1, x + w, y + h, withAlpha(accent, 0x44));
        // Right-edge hair line
        ctx.fill(x + w - 1, y + 2, x + w, y + h - 1, withAlpha(accent, 0x44));

        // Badge → content separator: 1 px
        if (cfg.iconEnabled) {
            ctx.fill(x + badgeW, y + 2, x + badgeW + 1, y + h - 1,
                    withAlpha(accent, 0xBB));
        }

        // ── Text ─────────────────────────────────────────────────
        int textY   = y + (h - 8) / 2;           // vertically centred
        int contentX = x + badgeW + (cfg.iconEnabled ? 1 : 0) + 5;

        // Symbol centred in badge
        if (cfg.iconEnabled) {
            int symX = x + (badgeW - tr.getWidth(cfg.currencySymbol)) / 2;
            ctx.drawText(tr, cfg.currencySymbol, symX, textY, accent, true);
        }

        // "LABEL:"
        String lbl = hudLabel() + ":";
        ctx.drawText(tr, lbl, contentX, textY, labelC, false);
        contentX += tr.getWidth(lbl) + 7;

        // Value (flash animation, no scale on tier 1)
        drawAnimatedValue(ctx, tr, valueText, contentX, textY,
                valueC, accent, cfg.animationEnabled, false);
    }

    // ═════════════════════════════════════════════════════════════
    // TIER 2  "Cartera"  ─  Split card  (h = 44 px)
    //
    //  ╔══════════════════╦══════════════════════════╗  ← 2 px accent
    //  ║    $             ║                          ║
    //  ║    DINERO        ║        1,300             ║
    //  ╚══════════════════╩══════════════════════════╝  ← 2 px dim
    //  corner accents ↗↗↗                          ↗↗
    //
    //  • LEFT panel: accent-tinted bg, symbol + label centred
    //  • 2 px vertical accent divider
    //  • RIGHT panel: dark bg, value centred vertically
    //  • corner ornaments: 2×2 accent squares at all 4 corners
    // ═════════════════════════════════════════════════════════════

    private void renderTier2(DrawContext ctx, TextRenderer tr, MoneyHudConfig cfg,
                              int x, int y, int w, int h, String valueText) {

        // ── Tier 2 palette: yellow ────────────────────────────────
        final int accent = 0xFFFFD700;   // vivid gold-yellow – bars, divider, $
        final int labelC = 0xFFFFCC44;   // warm amber – label text
        final int valueC = 0xFFFFFFFF;   // white – value text

        // Left panel width: wide enough for $ and LABEL, centred
        int leftW = tier2LeftWidth(tr, cfg);
        int divW  = 2;

        // ── Background ───────────────────────────────────────────
        if (cfg.backgroundEnabled) {
            int bgA = alphaFromOpacity(cfg.backgroundOpacity);
            // Right panel: pure dark
            ctx.fill(x, y, x + w, y + h, argb(bgA, 0x0A, 0x0A, 0x0A));
            // Left panel: accent tint overlay
            ctx.fill(x, y + 2, x + leftW, y + h - 2, accentTint(accent, 0x20));
        }

        // ── Top / bottom bars ────────────────────────────────────
        ctx.fill(x, y,         x + w, y + 2,     accent);                      // top 2 px
        ctx.fill(x, y + h - 2, x + w, y + h,     withAlpha(accent, 0x44));     // bottom 2 px dim

        // ── Outer left + right edge lines ────────────────────────
        ctx.fill(x,         y + 2, x + 1,     y + h - 2, withAlpha(accent, 0x66));
        ctx.fill(x + w - 1, y + 2, x + w,     y + h - 2, withAlpha(accent, 0x33));

        // ── Vertical divider (2 px) ───────────────────────────────
        ctx.fill(x + leftW, y + 2, x + leftW + divW, y + h - 2,
                withAlpha(accent, 0xCC));

        // ── Inner top highlight on left panel ────────────────────
        ctx.fill(x + 1, y + 2, x + leftW - 1, y + 5,
                argb(0x18, 0xFF, 0xFF, 0xFF));

        // ── Corner ornaments (2×2 accent squares) ────────────────
        corner(ctx, accent, x,         y,         2, 0xFF);   // top-left
        corner(ctx, accent, x + w - 2, y,         2, 0xFF);   // top-right
        corner(ctx, accent, x,         y + h - 2, 2, 0x88);   // bottom-left
        corner(ctx, accent, x + w - 2, y + h - 2, 2, 0x88);   // bottom-right

        // ── LEFT panel: symbol (upper) + label (lower) ───────────
        int leftCentreX = x + leftW / 2;
        int symY  = y + 10;
        int lblY  = y + 27;

        if (cfg.iconEnabled) {
            int symX = leftCentreX - tr.getWidth(cfg.currencySymbol) / 2;
            // Fake glow: draw offset copy in dimmer accent first
            ctx.drawText(tr, cfg.currencySymbol, symX + 1, symY + 1,
                    withAlpha(accent, 0x55), false);
            ctx.drawText(tr, cfg.currencySymbol, symX, symY, accent, false);
        }

        String lblUp = hudLabel().toUpperCase();
        int lblX = leftCentreX - tr.getWidth(lblUp) / 2;
        ctx.drawText(tr, lblUp, lblX, lblY, labelC, false);

        // ── RIGHT panel: value centred ────────────────────────────
        int rightPanelX  = x + leftW + divW;
        int rightPanelW  = w - leftW - divW;
        int valueX = rightPanelX + (rightPanelW - tr.getWidth(valueText)) / 2;
        int valueY = y + (h - 8) / 2;

        drawAnimatedValue(ctx, tr, valueText, valueX, valueY,
                valueC, accent, cfg.animationEnabled, true);
    }

    // ═════════════════════════════════════════════════════════════
    // TIER 3  "Bóveda"  ─  Elite premium panel  (h = 64 px)
    //
    //  ▓▓▓▓ double outer shadow halo ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
    //  ████ 3 px gold + 1 px teal top bar ████████████████████████
    //  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ glass highlight
    //  ║ ── D I N E R O (spaced, teal) ── ║ ← header, purple tint
    //  ║ ──────────────────────────────── ║ ← separator + teal ticks
    //  ║                                  ║
    //  ║       $         1,300            ║ ← value 1.4× scale
    //  ║                                  ║
    //  ████ 1 px dim gold + 2 px teal bottom bar ███████████████
    //  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
    //
    //  Left strip: teal 2 px | Right strip: purple 2 px
    //  L-corner marks at all 4 corners (gold top, teal bottom)
    // ═════════════════════════════════════════════════════════════

    private void renderTier3(DrawContext ctx, TextRenderer tr, MoneyHudConfig cfg,
                              int x, int y, int w, int h, String valueText) {

        // ── Palette: gold · purple · teal ────────────────────────
        final int gold   = 0xFFFFD700;
        final int purple = 0xFF9B30FF;
        final int teal   = 0xFF00E5CC;
        final int white  = 0xFFFFFFFF;

        float flash     = cfg.animationEnabled ? animator.getFlashIntensity() : 0f;
        int flashGold   = blendToWhite(gold,   flash * 0.55f);
        int flashTeal   = blendToWhite(teal,   flash * 0.45f);
        int flashPurple = blendToWhite(purple, flash * 0.40f);

        // ── Zone layout ───────────────────────────────────────────
        final int topBarH = 4;   // 3 px gold + 1 px teal
        final int botBarH = 3;   // 1 px dim gold + 2 px teal
        final int headerH = 20;
        final int sepY    = y + topBarH + headerH;

        // ── Double shadow halo ────────────────────────────────────
        ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, argb(0x28, 0x00, 0x00, 0x00));
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, argb(0x55, 0x00, 0x00, 0x00));

        // ── Background ───────────────────────────────────────────
        if (cfg.backgroundEnabled) {
            int bgA = Math.max(0xCC, alphaFromOpacity(cfg.backgroundOpacity));
            // Dark purple-tinted base
            ctx.fill(x, y, x + w, y + h, argb(bgA, 0x08, 0x05, 0x12));
            // Strong purple header tint
            ctx.fill(x + 2, y + topBarH, x + w - 2, sepY, accentTint(purple, 0x55));
            // Glass highlight just below top bar
            ctx.fill(x + 2, y + topBarH, x + w - 2, y + topBarH + 2,
                    argb(0x22, 0xFF, 0xFF, 0xFF));
            // Subtle gold tint on value zone
            ctx.fill(x + 2, sepY + 2, x + w - 2, y + h - botBarH,
                    accentTint(gold, 0x09));
        }

        // ── Top bar: 3 px gold + 1 px teal ───────────────────────
        ctx.fill(x, y,     x + w, y + 3,       flashGold);
        ctx.fill(x, y + 3, x + w, y + topBarH, withAlpha(flashTeal, 0xCC));

        // ── Bottom bar: 1 px dim gold + 2 px teal ────────────────
        ctx.fill(x, y + h - botBarH,     x + w, y + h - botBarH + 1,
                withAlpha(gold, 0x55));
        ctx.fill(x, y + h - botBarH + 1, x + w, y + h,
                withAlpha(flashTeal, 0xBB));

        // ── Side strips: left teal 2 px, right purple 2 px ───────
        int stripA = (int)(0x70 + flash * 0x50);
        ctx.fill(x,         y + topBarH, x + 2,     y + h - botBarH,
                withAlpha(teal,   stripA));
        ctx.fill(x + w - 2, y + topBarH, x + w,     y + h - botBarH,
                withAlpha(purple, stripA));

        // ── L-corner marks (5 px arms): gold top, teal bottom ────
        lCorner(ctx, flashGold, x + 2,     y + topBarH,     5, true,  true,  0xDD);
        lCorner(ctx, flashGold, x + w - 2, y + topBarH,     5, false, true,  0xDD);
        lCorner(ctx, teal,      x + 2,     y + h - botBarH, 5, true,  false, 0x99);
        lCorner(ctx, teal,      x + w - 2, y + h - botBarH, 5, false, false, 0x99);

        // ── Header: flanking gold bars + spaced teal label ────────
        final int CHAR_GAP = 3;
        String rawLabel = hudLabel().toUpperCase();
        int spacedW = spacedTextWidth(tr, rawLabel, CHAR_GAP);
        int labelX  = x + (w - spacedW) / 2;
        int labelY  = y + topBarH + 6;
        int barY    = labelY + 3;   // visually centred beside label mid-line

        // Flanking "──" bars on each side of the label text
        int barL = labelX - 5;
        int barR = labelX + spacedW + 5;
        if (barL > x + 12) {
            ctx.fill(x + 12, barY, barL, barY + 1, withAlpha(flashGold, 0x99));
        }
        if (barR < x + w - 12) {
            ctx.fill(barR, barY, x + w - 12, barY + 1, withAlpha(flashGold, 0x99));
        }

        // Purple glow pass (1 px offset shadow)
        drawSpacedText(ctx, tr, rawLabel, labelX + 1, labelY + 1,
                withAlpha(flashPurple, 0xAA), CHAR_GAP, false);
        // Teal main label
        drawSpacedText(ctx, tr, rawLabel, labelX, labelY, flashTeal, CHAR_GAP, false);

        // ── Separator: gold line + purple dim + teal tick marks ───
        ctx.fill(x + 10, sepY,     x + w - 10, sepY + 1,
                withAlpha(flashGold,   0xCC));
        ctx.fill(x + 10, sepY + 1, x + w - 10, sepY + 2,
                withAlpha(flashPurple, 0x44));
        // Teal tick marks at both ends of separator
        ctx.fill(x + 10,     sepY - 1, x + 12,     sepY + 3, withAlpha(teal, 0xBB));
        ctx.fill(x + w - 12, sepY - 1, x + w - 10, sepY + 3, withAlpha(teal, 0xBB));

        // ── Value: 1.4× base scale + pop animation ────────────────
        final float baseScale = 1.4f;
        float popScale   = cfg.animationEnabled ? animator.getValueScale() : 1.0f;
        float totalScale = baseScale * popScale;

        int symW   = cfg.iconEnabled ? tr.getWidth(cfg.currencySymbol + " ") : 0;
        int blockW = symW + tr.getWidth(valueText);
        int blockX = x + (w - blockW) / 2;

        int valueZoneTop = sepY + 2;
        int valueZoneBot = y + h - botBarH;
        int blockY = valueZoneTop + (valueZoneBot - valueZoneTop - 8) / 2;

        ctx.getMatrices().push();
        if (totalScale != 1.0f) {
            float px = blockX + blockW / 2f;
            float py = blockY + 4f;
            ctx.getMatrices().translate( px,  py, 0);
            ctx.getMatrices().scale(totalScale, totalScale, 1f);
            ctx.getMatrices().translate(-px, -py, 0);
        }

        if (cfg.iconEnabled) {
            // $ glow pass (offset shadow in dim gold)
            ctx.drawText(tr, cfg.currencySymbol, blockX + 1, blockY + 1,
                    withAlpha(flashGold, 0x55), false);
            ctx.drawText(tr, cfg.currencySymbol + " ", blockX, blockY,
                    flashGold, true);
            ctx.drawText(tr, valueText, blockX + symW, blockY,
                    blendToWhite(white, flash * 0.2f), true);
        } else {
            ctx.drawText(tr, valueText, blockX, blockY,
                    blendToWhite(white, flash * 0.2f), true);
        }

        ctx.getMatrices().pop();
    }

    // ═════════════════════════════════════════════════════════════
    // Shared helper – animated value text (Tier 1 & 2)
    // ═════════════════════════════════════════════════════════════

    private void drawAnimatedValue(DrawContext ctx, TextRenderer tr,
                                   String text, int x, int y,
                                   int baseColor, int accentColor,
                                   boolean animEnabled, boolean allowScale) {
        float pop   = (animEnabled && allowScale) ? animator.getValueScale() : 1.0f;
        float flash = animEnabled ? animator.getFlashIntensity() : 0f;
        int   color = blendToWhite(blendColors(baseColor, accentColor, flash * 0.5f),
                flash * 0.2f);

        if (pop != 1.0f) {
            ctx.getMatrices().push();
            float px = x + tr.getWidth(text) / 2f;
            float py = y + 4f;
            ctx.getMatrices().translate( px,  py, 0);
            ctx.getMatrices().scale(pop, pop, 1f);
            ctx.getMatrices().translate(-px, -py, 0);
            ctx.drawText(tr, text, x, y, color, true);
            ctx.getMatrices().pop();
        } else {
            ctx.drawText(tr, text, x, y, color, true);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // Shared helper – spaced character rendering (Tier 3 header)
    // ═════════════════════════════════════════════════════════════

    /** Draws text with extra pixel gap between each character. */
    private static void drawSpacedText(DrawContext ctx, TextRenderer tr,
                                       String text, int x, int y,
                                       int color, int charGap, boolean shadow) {
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            ctx.drawText(tr, ch, cx, y, color, shadow);
            cx += tr.getWidth(ch) + charGap;
        }
    }

    /** Width of a string rendered with extra inter-character gap. */
    private static int spacedTextWidth(TextRenderer tr, String text, int charGap) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            total += tr.getWidth(String.valueOf(text.charAt(i)));
            if (i < text.length() - 1) total += charGap;
        }
        return total;
    }

    // ═════════════════════════════════════════════════════════════
    // Shared helpers – corner ornaments
    // ═════════════════════════════════════════════════════════════

    /** Draws a size×size filled square at (x, y) in accent color with given alpha. */
    private static void corner(DrawContext ctx, int accent, int x, int y, int size, int alpha) {
        ctx.fill(x, y, x + size, y + size, withAlpha(accent, alpha));
    }

    /**
     * Draws an L-shaped corner mark of `size` px arms at corner point (cx, cy).
     *
     * @param right  true = L opens rightward  (horizontal arm goes right)
     * @param down   true = L opens downward   (vertical arm goes down)
     */
    private static void lCorner(DrawContext ctx, int accent,
                                 int cx, int cy, int size,
                                 boolean right, boolean down, int alpha) {
        int x1 = right ? cx : cx - size;
        int x2 = right ? cx + size : cx;
        int y1 = down  ? cy : cy - size;
        int y2 = down  ? cy + size : cy;
        // Horizontal arm: sits at the corner end (top when going down, bottom when going up)
        int hy = down ? y1 : y2 - 1;
        ctx.fill(x1, hy, x2, hy + 1, withAlpha(accent, alpha));
        // Vertical arm (1 px wide) along the near edge
        int vx = right ? x1 : x2 - 1;
        ctx.fill(vx, y1, vx + 1, y2, withAlpha(accent, alpha));
    }

    // ═════════════════════════════════════════════════════════════
    // Dynamic panel width calculators
    // ═════════════════════════════════════════════════════════════

    private int computeTier1Width(TextRenderer tr, MoneyHudConfig cfg, String val) {
        int badgeW = cfg.iconEnabled
                ? Math.max(18, 5 + tr.getWidth(cfg.currencySymbol) + 5)
                : 0;
        int sep    = cfg.iconEnabled ? 1 : 0;
        int content = 5 + tr.getWidth(hudLabel() + ": ") + 7 + tr.getWidth(val) + 6;
        return Math.max(90, badgeW + sep + content);
    }

    private int computeTier2Width(TextRenderer tr, MoneyHudConfig cfg, String val) {
        int leftW  = tier2LeftWidth(tr, cfg);
        int rightW = 8 + tr.getWidth(val) + 8;
        return Math.max(100, leftW + 2 + rightW);
    }

    /** Width of the left icon panel in Tier 2. */
    private int tier2LeftWidth(TextRenderer tr, MoneyHudConfig cfg) {
        int symW = cfg.iconEnabled ? 8 + tr.getWidth(cfg.currencySymbol) + 8 : 0;
        int lblW = 8 + tr.getWidth(hudLabel().toUpperCase()) + 8;
        return Math.max(35, Math.max(symW, lblW));
    }

    private int computeTier3Width(TextRenderer tr, MoneyHudConfig cfg, String val) {
        final int CHAR_GAP = 3;
        int headerW = spacedTextWidth(tr, hudLabel().toUpperCase(), CHAR_GAP) + 48;
        int symW    = cfg.iconEnabled ? tr.getWidth(cfg.currencySymbol + " ") : 0;
        int valueW  = symW + tr.getWidth(val) + 40;
        return Math.max(130, Math.max(headerW, valueW));
    }

    // ═════════════════════════════════════════════════════════════
    // Translation helper
    // ═════════════════════════════════════════════════════════════

    /**
     * Returns the HUD label in the player's current language.
     * Keys: moneyhud.hud.label → "Dinero" / "Money" / "Geld"
     */
    private static String hudLabel() {
        return I18n.translate("moneyhud.hud.label");
    }

    // ═════════════════════════════════════════════════════════════
    // Color utilities (ARGB ints throughout)
    // ═════════════════════════════════════════════════════════════

    /** Replace the alpha channel of an ARGB color. */
    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /** Build an ARGB int from separate channels (all 0–255). */
    private static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Convert an opacity float (0–1) to an alpha byte (0–255). */
    private static int alphaFromOpacity(float opacity) {
        return MathHelper.clamp((int) (opacity * 255), 0, 255);
    }

    /**
     * Returns a fill color that is the accent color but with the given alpha,
     * suitable for a semi-transparent accent tint over a dark background.
     */
    private static int accentTint(int accent, int alpha) {
        return argb(alpha,
                (accent >> 16) & 0xFF,
                (accent >>  8) & 0xFF,
                 accent        & 0xFF);
    }

    /**
     * Linearly blend two ARGB colors channel-by-channel.
     * t = 0 → a fully, t = 1 → b fully.
     */
    private static int blendColors(int a, int b, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int aa = (a >> 24) & 0xFF, ar = (a >> 16) & 0xFF,
            ag = (a >>  8) & 0xFF, ab =  a        & 0xFF;
        int ba = (b >> 24) & 0xFF, br = (b >> 16) & 0xFF,
            bg = (b >>  8) & 0xFF, bb =  b        & 0xFF;
        return argb((int)(aa + (ba - aa) * t),
                    (int)(ar + (br - ar) * t),
                    (int)(ag + (bg - ag) * t),
                    (int)(ab + (bb - ab) * t));
    }

    /** Lighten an ARGB color toward white by factor t (0 = unchanged, 1 = white). */
    private static int blendToWhite(int argb, float t) {
        return blendColors(argb, 0xFFFFFFFF, MathHelper.clamp(t, 0f, 1f));
    }
}
