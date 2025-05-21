package com.code_space.code_space.entity;

public enum RoomStatus {
    SCHEDULED,      // Meeting is scheduled but not started
    WAITING,        // Waiting for host to start
    ACTIVE,         // Meeting is in progress
    ENDED,          // Meeting has ended
    CANCELLED       // Meeting was cancelled
}