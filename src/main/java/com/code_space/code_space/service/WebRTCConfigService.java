package com.code_space.code_space.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebRTCConfigService {

    @Value("${webrtc.stun.servers:stun:stun.l.google.com:19302,stun:stun1.l.google.com:19302}")
    private String stunServers;

    @Value("${webrtc.turn.servers:}")
    private String turnServers;

    @Value("${webrtc.turn.username:}")
    private String turnUsername;

    @Value("${webrtc.turn.credential:}")
    private String turnCredential;

    /**
     * Get ICE servers configuration for WebRTC
     */
    public Map<String, Object> getIceServersConfig() {
        List<Map<String, Object>> iceServers = new ArrayList<>();

        // Add STUN servers
        for (String stunServer : stunServers.split(",")) {
            if (!stunServer.trim().isEmpty()) {
                Map<String, Object> stunConfig = new HashMap<>();
                stunConfig.put("urls", stunServer.trim());
                iceServers.add(stunConfig);
            }
        }

        // Add TURN servers if configured
        if (!turnServers.isEmpty()) {
            for (String turnServer : turnServers.split(",")) {
                if (!turnServer.trim().isEmpty()) {
                    Map<String, Object> turnConfig = new HashMap<>();
                    turnConfig.put("urls", turnServer.trim());
                    turnConfig.put("username", turnUsername);
                    turnConfig.put("credential", turnCredential);
                    iceServers.add(turnConfig);
                }
            }
        }

        return Map.of(
                "iceServers", iceServers,
                "iceCandidatePoolSize", 10,
                "iceTransportPolicy", "all"
        );
    }

    /**
     * Get complete WebRTC configuration for peer connections
     */
    public Map<String, Object> getWebRTCConfiguration() {
        Map<String, Object> config = new HashMap<>(getIceServersConfig());

        // Add additional peer connection configuration
        config.put("bundlePolicy", "max-bundle");
        config.put("rtcpMuxPolicy", "require");

        // Media constraints
        Map<String, Object> mediaConstraints = Map.of(
                "audio", Map.of(
                        "echoCancellation", true,
                        "noiseSuppression", true,
                        "autoGainControl", true
                ),
                "video", Map.of(
                        "width", Map.of("ideal", 1280),
                        "height", Map.of("ideal", 720),
                        "frameRate", Map.of("ideal", 30)
                )
        );

        config.put("mediaConstraints", mediaConstraints);

        return config;
    }

    /**
     * Get screen sharing constraints
     */
    public Map<String, Object> getScreenShareConstraints() {
        return Map.of(
                "video", Map.of(
                        "mediaSource", "screen",
                        "width", Map.of("max", 1920),
                        "height", Map.of("max", 1080),
                        "frameRate", Map.of("max", 30)
                ),
                "audio", Map.of(
                        "mediaSource", "audioCapture",
                        "echoCancellation", false,
                        "noiseSuppression", false,
                        "autoGainControl", false
                )
        );
    }

    public Map<String, Object> getScreenShareConstraints(String quality) {
        Map<String, Object> videoConstraints;

        switch (quality) {
            case "low":
                videoConstraints = Map.of(
                        "mediaSource", "screen",
                        "width", Map.of("max", 640),
                        "height", Map.of("max", 480),
                        "frameRate", Map.of("max", 15)
                );
                break;
            case "high":
                videoConstraints = Map.of(
                        "mediaSource", "screen",
                        "width", Map.of("max", 1920),
                        "height", Map.of("max", 1080),
                        "frameRate", Map.of("max", 30)
                );
                break;
            default: // medium
                videoConstraints = Map.of(
                        "mediaSource", "screen",
                        "width", Map.of("max", 1280),
                        "height", Map.of("max", 720),
                        "frameRate", Map.of("max", 24)
                );
                break;
        }

        return Map.of(
                "video", videoConstraints,
                "audio", Map.of(
                        "mediaSource", "audioCapture",
                        "echoCancellation", false,
                        "noiseSuppression", false,
                        "autoGainControl", false
                )
        );
    }
}