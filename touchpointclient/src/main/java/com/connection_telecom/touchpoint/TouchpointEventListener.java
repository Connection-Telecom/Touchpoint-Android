package com.connection_telecom.touchpoint;

/**
 * Created by michaeld on 2017-02-22.
 */
public interface TouchpointEventListener {
    void onChatCreated(String chatId);
    void onAgentTyping(boolean isTyping);
    void onMessage(String type, String message);
    void onChatClosed();
    void onError(String error);
    void onDisconnect();
}
