package com.elthisboy.moneyhud.hud;

/**
 * Tracks animation state for the money HUD.
 *
 * Two distinct animation modes:
 *
 *  POP    – single change detected: value text scales up then eases back (0.45 s).
 *           Gives satisfying feedback for occasional updates.
 *
 *  STREAK – a second change arrives while the first pop is still active.
 *           Instead of repeating the pop every tick (which looks frantic when money
 *           ticks up every second), the display switches to a slow rhythmic pulse
 *           with NO scale pop. The pulse fades out 1 s after the last change.
 *
 * State machine:  IDLE ──change──► POP ──change while active──► STREAK
 *                 POP  ──expires──► IDLE
 *                 STREAK ──1 s no change──► IDLE
 */
public class HudAnimator {

    // ── Timing constants ──────────────────────────────────────────
    private static final float POP_DURATION   = 0.45f;   // single-pop length (s)
    private static final float STREAK_DECAY   = 1.00f;   // streak hold after last change (s)
    private static final float STREAK_PULSE   = 0.70f;   // pulse cycle length in streak mode (s)

    // ── Intensity constants ───────────────────────────────────────
    private static final float MAX_POP        = 0.28f;   // extra scale at pop peak
    private static final float MAX_FLASH      = 0.55f;   // flash brightness for POP
    private static final float STREAK_FLASH   = 0.40f;   // max flash brightness in STREAK

    // ── State ─────────────────────────────────────────────────────
    private enum Mode { IDLE, POP, STREAK }

    private Mode  mode          = Mode.IDLE;
    private float timeRemaining = 0f;    // POP countdown
    private float streakTimer   = 0f;    // STREAK: time remaining after last change
    private float pulsePhase    = 0f;    // STREAK: accumulates real time for sin pulse

    private int   lastKnownValue = Integer.MIN_VALUE;
    private boolean increased    = false;

    // ── Public API ────────────────────────────────────────────────

    /**
     * Called every render frame with the current scoreboard value.
     *
     * @param currentValue  value read this frame
     * @param deltaSeconds  wall-clock seconds since last call
     */
    public void tick(int currentValue, float deltaSeconds) {
        boolean changed = lastKnownValue != Integer.MIN_VALUE
                && currentValue != lastKnownValue;

        if (changed) {
            increased = currentValue > lastKnownValue;

            switch (mode) {
                case IDLE -> {
                    // First change: start a normal pop
                    mode = Mode.POP;
                    timeRemaining = POP_DURATION;
                }
                case POP -> {
                    // Second change arrived while pop is active → streak
                    mode = Mode.STREAK;
                    streakTimer = STREAK_DECAY;
                    pulsePhase  = 0f;
                }
                case STREAK -> {
                    // Already in streak: just reset the decay window
                    streakTimer = STREAK_DECAY;
                }
            }
        }

        lastKnownValue = currentValue;

        // Advance timers
        switch (mode) {
            case POP -> {
                timeRemaining = Math.max(0f, timeRemaining - deltaSeconds);
                if (timeRemaining == 0f) mode = Mode.IDLE;
            }
            case STREAK -> {
                pulsePhase  += deltaSeconds;
                streakTimer  = Math.max(0f, streakTimer - deltaSeconds);
                if (streakTimer == 0f) mode = Mode.IDLE;
            }
            case IDLE -> { /* nothing */ }
        }
    }

    /**
     * Scale multiplier for the value text.
     * Returns > 1.0 only during POP mode; STREAK deliberately returns 1.0
     * (no pop) to avoid the frantic repeated-scale look.
     */
    public float getValueScale() {
        if (mode != Mode.POP || timeRemaining <= 0f) return 1.0f;
        float progress = timeRemaining / POP_DURATION;   // 1 → 0
        float envelope = (float) Math.sin(progress * Math.PI);
        return 1.0f + envelope * MAX_POP;
    }

    /**
     * Flash/brightness intensity (0 = none, MAX_FLASH = peak).
     *
     * POP:    sharp sine arc, peaks at start, fades to 0.
     * STREAK: slow sine pulse that fades out as the streak decays.
     */
    public float getFlashIntensity() {
        return switch (mode) {
            case POP -> {
                float progress = timeRemaining / POP_DURATION;
                yield (float) Math.sin(progress * Math.PI) * MAX_FLASH;
            }
            case STREAK -> {
                // Slow pulse: ranges 0.15–1.0, shaped by decay envelope
                float pulse       = (float)(Math.sin(pulsePhase * 2 * Math.PI / STREAK_PULSE)
                                            * 0.425 + 0.575);   // 0.15 → 1.0
                float fadeEnvelope = streakTimer / STREAK_DECAY; // 1 → 0
                yield pulse * STREAK_FLASH * fadeEnvelope;
            }
            case IDLE -> 0f;
        };
    }

    /** True while any animation is playing. */
    public boolean isAnimating() { return mode != Mode.IDLE; }

    /** True while in STREAK mode (rapid successive changes). */
    public boolean isStreaking() { return mode == Mode.STREAK; }

    /** True if the last detected change was an increase. */
    public boolean wasIncrease() { return increased; }
}
