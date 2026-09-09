package com.ultraop.mocap.recording;

/** A player chat message recorded at a specific playback tick. */
public record ChatMessageFrame(long tick, String messageJson) {
    public ChatMessageFrame {
        if (messageJson == null) messageJson = "{}";
    }
}
