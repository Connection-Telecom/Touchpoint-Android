package com.connection_telecom.touchpoint;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketOptions;
import im.delight.android.ddp.Meteor;
import im.delight.android.ddp.MeteorCallback;
import im.delight.android.ddp.ResultListener;
import im.delight.android.ddp.SubscribeListener;

/**
 * Created by michaeld on 2017-02-22.
 */
public class TouchpointClient {
    private Context context;

    private String customerId;
    private String topic = "<no topic>";
    private String team = "default";
    private String signedContextString;
    private String signature;
    private String unsignedContextString;

    private Meteor meteor;

    private String chatId;
    private List<TouchpointEventListener> listeners;

    private boolean agentIsTyping = false;
    private boolean userIsTyping = false;

    private static ObjectMapper mapper = new ObjectMapper();

    //----------

    public TouchpointClient(Context context, String customerId, String topic, String team) {
        this.context = context;
        this.customerId = customerId;
        this.topic = topic;
        this.team = team;
        listeners = new ArrayList<>();
    }

    public TouchpointClient setSignedContext(String signedContextString, String signature) {
        if (meteor != null) {
            throw new IllegalStateException("Cannot setSignedContext() after connect()");
        }
        this.signedContextString = signedContextString;
        this.signature = signature;
        return this;
    }

    public TouchpointClient setUnsignedContext(String unsignedContextString) {
        if (meteor != null) {
            throw new IllegalStateException("Cannot setUnsignedContext() after connect()");
        }
        this.unsignedContextString = unsignedContextString;
        return this;
    }

    public TouchpointClient addEventListener(TouchpointEventListener listener) {
        listeners.add(listener);
        return this;
    }

    //----------

    public void connect() {
        if (meteor != null) throw new IllegalStateException("Can't call connect() more than once");
        meteor = new Meteor(context, "wss://touchpoint.telviva.com/websocket", "1", new WebSocketOptions());
        meteor.setCallback(new Callback());
    }

    private String parseJsonString(String jsonString) {
        try {
            JsonNode node = mapper.readValue(jsonString, JsonNode.class);
            if (node.isTextual()) return node.getTextValue();
        } catch (IOException e) {}
        fail("Unexpected response from Touchpoint");
        return null;
    }

    public void sendMessage(String message) {
        if (meteor == null) {
            throw new IllegalStateException("Cannot sendMessage() before connect()");
        }
        if (!meteor.isConnected()) return;
        meteor.call("postMessageAsUser", new Object[] {chatId, message});
        userIsTyping = false;
    }

    public void setUserTyping(boolean isTyping) {
        if (meteor == null) {
            throw new IllegalStateException("Cannot setUserTyping() before connect()");
        }
        if (isTyping != userIsTyping) {
            meteor.call("setUserIsTyping", new Object[] {chatId, isTyping});
        }
        userIsTyping = isTyping;
    }

    public boolean getUserTyping() {
        return userIsTyping;
    }

    public boolean getAgentTyping() {
        return agentIsTyping;
    }

    private class Callback implements MeteorCallback {
        @Override
        public void onConnect(boolean signedInAutomatically) {
            meteor.call("createChat", new Object[] {customerId, topic, team, "text", signedContextString, signature, unsignedContextString}, new ResultListener() {
                @Override
                public void onSuccess(String result) {
                    chatId = parseJsonString(result);
                    if (chatId != null) {
                        for (TouchpointEventListener listener : listeners) {
                            listener.onChatCreated(chatId);
                        }
                        subscribe();
                    }
                }

                @Override
                public void onError(String error, String reason, String details) {
                    fail("Error creating chat: " + error + ";" + reason + "; " + details);
                }
            });
        }

        @Override
        public void onDisconnect(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification webSocketCloseNotification, String s) {
            for (TouchpointEventListener listener : listeners) {
                listener.onDisconnect();
            }
        }

        @Override
        public void onDataAdded(String collectionName, String documentId, String newValuesJson) {
            if (collectionName.equals("messages")) {
                try {
                    JsonNode node = mapper.readValue(newValuesJson, JsonNode.class);
                    String type = node.get("type").getTextValue();
                    String message = node.get("message").getTextValue();
                    for (TouchpointEventListener listener : listeners) {
                        listener.onMessage(type, message);
                    }
                } catch (IOException e) {
                    fail("Unexpected event from Touchpoint");
                }
            }
        }

        @Override
        public void onDataChanged(String collectionName, String documentId, String updateValuesJson, String removedValuesJson) {
            if (collectionName.equals("chats")) {
                try {
                    JsonNode node = mapper.readValue(updateValuesJson, JsonNode.class);
                    if (node.has("agentIsTyping")) {
                        boolean newAIT = node.get("agentIsTyping").getBooleanValue();
                        if (newAIT != agentIsTyping) {
                            for (TouchpointEventListener listener : listeners) {
                                listener.onAgentTyping(newAIT);
                            }
                        }
                        agentIsTyping = newAIT;
                    }
                    if (node.has("isClosed") && node.get("isClosed").getBooleanValue()) {
                        for (TouchpointEventListener listener : listeners) {
                            listener.onChatClosed();
                        }
                        meteor.disconnect();
                    }
                } catch (IOException e) {
                    fail("Unexpected event from Touchpoint");
                }
            }
        }

        @Override
        public void onDataRemoved(String collectionName, String documentId) {
        }

        @Override
        public void onException(Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    private void subscribe() {
        meteor.subscribe("userChat", new Object[] {chatId}, new SubscribeListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String error, String reason, String details) {
                fail("Error subscribing to chat: " + error + ";" + reason + "; " + details);
                meteor.disconnect();
            }
        });
    }

    private void fail(String message) {
        for (TouchpointEventListener listener : listeners) {
            listener.onError(message);
        }
        meteor.disconnect();
    }

    public void destroy() {
        listeners.clear();
        meteor.disconnect();
    }

    //----------

    private static AsyncHttpClient httpClient = new AsyncHttpClient();

    public static void getChannelAvailability(String customer, String team, final ChannelAvailabilityCallback callback) {
        httpClient.get("https://touchpoint.telviva.com/api/customers/" + customer + "/teams/" + team + "/channels", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                List<String> availableChannels = new ArrayList<>();
                List<String> unavailableChannels = new ArrayList<>();
                try {
                    JsonNode node = mapper.readValue(new String(responseBody), JsonNode.class);
                    for (Iterator<JsonNode> it = node.getElements(); it.hasNext();) {
                        JsonNode obj = it.next();
                        if (obj.get("available").getBooleanValue()) {
                            availableChannels.add(obj.get("channel").getTextValue());
                        } else {
                            unavailableChannels.add(obj.get("channel").getTextValue());
                        }
                    }
                    callback.onSuccess(availableChannels, unavailableChannels);
                } catch (IOException e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                callback.onFailure(error);
            };
        });
    }
}
