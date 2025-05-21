package com.code_space.code_space.dto.webrtc;

public class MediaControlMessage {
    private String participantId;
    private String roomId;
    private MediaType mediaType;
    private boolean enabled;
    private String streamId;

    public enum MediaType {
        AUDIO, VIDEO, SCREEN_SHARE
    }

    // Constructors
    public MediaControlMessage() {}

    public MediaControlMessage(String participantId, String roomId, MediaType mediaType, boolean enabled) {
        this.participantId = participantId;
        this.roomId = roomId;
        this.mediaType = mediaType;
        this.enabled = enabled;
    }

    // Getters and Setters
    public String getParticipantId() { return participantId; }
    public void setParticipantId(String participantId) { this.participantId = participantId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getStreamId() { return streamId; }
    public void setStreamId(String streamId) { this.streamId = streamId; }
}