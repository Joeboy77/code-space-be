package com.code_space.code_space.entity;

public enum ReactionType {
    THUMBS_UP("👍", "thumbs-up"),
    HEART("❤️", "heart"),
    LAUGH("😄", "laugh"),
    CLAP("👏", "clap"),
    PARTY("🎉", "party"),
    THINKING("🤔", "thinking"),
    SURPRISED("😮", "surprised"),
    FIRE("🔥", "fire");

    private final String emoji;
    private final String code;

    ReactionType(String emoji, String code) {
        this.emoji = emoji;
        this.code = code;
    }

    public String getEmoji() { return emoji; }
    public String getCode() { return code; }

    public static ReactionType fromCode(String code) {
        for (ReactionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown reaction code: " + code);
    }
}
