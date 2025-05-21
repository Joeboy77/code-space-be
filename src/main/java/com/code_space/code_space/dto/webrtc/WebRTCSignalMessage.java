package com.code_space.code_space.dto.webrtc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WebRTCSignalMessage {
    private String type;
    private String roomId;
    private String fromParticipantId;
    private String toParticipantId;
    private Object data;
    private Long timestamp;

    @JsonCreator
    public WebRTCSignalMessage(
            @JsonProperty("type") String type,
            @JsonProperty("roomId") String roomId,
            @JsonProperty("fromParticipantId") String fromParticipantId,
            @JsonProperty("toParticipantId") String toParticipantId,
            @JsonProperty("data") Object data
    ) {
        this.type = type;
        this.roomId = roomId;
        this.fromParticipantId = fromParticipantId;
        this.toParticipantId = toParticipantId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getFromParticipantId() { return fromParticipantId; }
    public void setFromParticipantId(String fromParticipantId) { this.fromParticipantId = fromParticipantId; }

    public String getToParticipantId() { return toParticipantId; }
    public void setToParticipantId(String toParticipantId) { this.toParticipantId = toParticipantId; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}