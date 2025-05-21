package com.code_space.code_space.entity;

public enum ParticipantStatus {
    INVITED,        // Invited but not joined
    WAITING,        // In waiting room
    JOINED,         // Currently in meeting
    LEFT,           // Left the meeting
    REMOVED         // Removed by host
}