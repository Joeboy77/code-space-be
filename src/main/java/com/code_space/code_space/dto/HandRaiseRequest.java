package com.code_space.code_space.dto;

public class HandRaiseRequest {
    private boolean raised;

    public HandRaiseRequest() {}

    public HandRaiseRequest(boolean raised) {
        this.raised = raised;
    }

    public boolean isRaised() { return raised; }
    public void setRaised(boolean raised) { this.raised = raised; }
}
