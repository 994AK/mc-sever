package net.leafmc.gomoku;

public record EnvironmentFeedbackProfile(
    String id,
    boolean actionbarEnabled,
    boolean particlesEnabled,
    boolean soundsEnabled,
    boolean winLineEnabled,
    boolean countdownEnabled
) {
    public static final String DEFAULT_ID = "soft";

    public static EnvironmentFeedbackProfile soft() {
        return new EnvironmentFeedbackProfile(DEFAULT_ID, true, true, true, true, true);
    }

    public static EnvironmentFeedbackProfile quiet() {
        return new EnvironmentFeedbackProfile("quiet", true, false, true, false, true);
    }
}
