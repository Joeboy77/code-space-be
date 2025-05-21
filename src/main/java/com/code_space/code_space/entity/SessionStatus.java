package com.code_space.code_space.entity;

public enum SessionStatus {
    WAITING,    // Session created, waiting for participants
    ACTIVE,     // Session active with participants
    PAUSED,     // Session temporarily paused
    ENDED       // Session ended
}