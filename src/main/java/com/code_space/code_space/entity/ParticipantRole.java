package com.code_space.code_space.entity;

public enum ParticipantRole {
    HOST,           // Meeting host (creator)
    CO_HOST,        // Co-host with host privileges
    PARTICIPANT,    // Regular participant
    GUEST           // Guest user (no account)
}