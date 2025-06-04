package com.code_space.code_space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ReactionRequest {
    @NotBlank(message = "Reaction type is required")
    @Pattern(regexp = "thumbs-up|heart|laugh|clap|party|thinking|surprised|fire",
            message = "Invalid reaction type")
    private String reactionType;

    public ReactionRequest() {}

    public ReactionRequest(String reactionType) {
        this.reactionType = reactionType;
    }

    public String getReactionType() { return reactionType; }
    public void setReactionType(String reactionType) { this.reactionType = reactionType; }
}