package com.code_space.code_space.dto.webrtc;

public class ScreenShareMessage {
    private String participantId;
    private String roomId;
    private String streamId;
    private boolean isSharing;
    private boolean isPinned;
    private String screenTitle;

    // Constructors
    public ScreenShareMessage() {}

    public ScreenShareMessage(String participantId, String roomId, String streamId, boolean isSharing) {
        this.participantId = participantId;
        this.roomId = roomId;
        this.streamId = streamId;
        this.isSharing = isSharing;
        this.isPinned = false;
    }

    // Getters and Setters
    public String getParticipantId() { return participantId; }
    public void setParticipantId(String participantId) { this.participantId = participantId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getStreamId() { return streamId; }
    public void setStreamId(String streamId) { this.streamId = streamId; }

    public boolean isSharing() { return isSharing; }
    public void setSharing(boolean isSharing) { this.isSharing = isSharing; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean isPinned) { this.isPinned = isPinned; }

    public String getScreenTitle() { return screenTitle; }
    public void setScreenTitle(String screenTitle) { this.screenTitle = screenTitle; }
}