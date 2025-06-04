package com.code_space.code_space.dto;

import com.code_space.code_space.entity.Room;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public class MeetingStateResponse {
    private Long roomId;
    private boolean isLocked;
    private boolean isRecording;
    private String activeSpeakerId;
    private long durationSeconds;
    private int participantCount;
    private int peakParticipants;
    private long totalMessages;
    private long totalReactions;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime recordingStartedAt;

    private List<String> raisedHands;
    private List<ReactionResponse> activeReactions;

    // Constructors
    public MeetingStateResponse() {}

    public MeetingStateResponse(Room room) {
        this.roomId = room.getId();
        this.isLocked = room.getIsLocked() != null ? room.getIsLocked() : false;
        this.isRecording = room.getIsRecording() != null ? room.getIsRecording() : false;
        this.activeSpeakerId = room.getActiveSpeakerId();
        this.durationSeconds = room.getMeetingDuration() != null ? room.getMeetingDuration() : 0;
        this.participantCount = room.getParticipantCount();
        this.peakParticipants = room.getPeakParticipants() != null ? room.getPeakParticipants() : 0;
        this.totalMessages = room.getTotalMessagesSent() != null ? room.getTotalMessagesSent() : 0;
        this.totalReactions = room.getTotalReactionsSent() != null ? room.getTotalReactionsSent() : 0;
        this.recordingStartedAt = room.getRecordingStartedAt();
    }

    // Getters and Setters
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    public boolean isRecording() { return isRecording; }
    public void setRecording(boolean recording) { isRecording = recording; }

    public String getActiveSpeakerId() { return activeSpeakerId; }
    public void setActiveSpeakerId(String activeSpeakerId) { this.activeSpeakerId = activeSpeakerId; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getParticipantCount() { return participantCount; }
    public void setParticipantCount(int participantCount) { this.participantCount = participantCount; }

    public int getPeakParticipants() { return peakParticipants; }
    public void setPeakParticipants(int peakParticipants) { this.peakParticipants = peakParticipants; }

    public long getTotalMessages() { return totalMessages; }
    public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }

    public long getTotalReactions() { return totalReactions; }
    public void setTotalReactions(long totalReactions) { this.totalReactions = totalReactions; }

    public LocalDateTime getRecordingStartedAt() { return recordingStartedAt; }
    public void setRecordingStartedAt(LocalDateTime recordingStartedAt) { this.recordingStartedAt = recordingStartedAt; }

    public List<String> getRaisedHands() { return raisedHands; }
    public void setRaisedHands(List<String> raisedHands) { this.raisedHands = raisedHands; }

    public List<ReactionResponse> getActiveReactions() { return activeReactions; }
    public void setActiveReactions(List<ReactionResponse> activeReactions) { this.activeReactions = activeReactions; }
}
