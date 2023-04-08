package com.example.splendormobilegame.websocket;

import android.util.Log;

import com.github.splendor_mobile_game.websocket.communication.ServerMessage;
import com.github.splendor_mobile_game.websocket.communication.UserMessage;
import com.github.splendor_mobile_game.websocket.handlers.ServerMessageType;
import com.github.splendor_mobile_game.websocket.utils.json.exceptions.JsonParserException;
import com.github.splendor_mobile_game.websocket.utils.reflection.Reflection;
import com.google.gson.Gson;

import java.net.URI;
import java.util.Map;

import tech.gusavila92.websocketclient.WebSocketClient;

public class CustomWebSocketClient extends WebSocketClient {

    private static CustomWebSocketClient instance;
    private final Map<ServerMessageType, Class<? extends UserReaction>> clientReactionsClasses;
    private final Gson gson;

    public CustomWebSocketClient(
            URI uri,
            Map<ServerMessageType, Class<? extends UserReaction>> clientReactionsClasses,
            int connectionTimeoutMs,
            int readTimeoutMs,
            int automaticReconnectionMs
    ) {
        super(uri);
        this.clientReactionsClasses = clientReactionsClasses;
        setConnectTimeout(connectionTimeoutMs);
        setReadTimeout(readTimeoutMs);
        enableAutomaticReconnection(automaticReconnectionMs);
        this.gson = new Gson();
    }

    public static void initialize(
            URI uri,
            Map<ServerMessageType, Class<? extends UserReaction>> clientReactionsClasses,
            int connectionTimeoutMs,
            int readTimeoutMs,
            int automaticReconnectionMs
    ) {
        instance = new CustomWebSocketClient(uri, clientReactionsClasses, connectionTimeoutMs, readTimeoutMs, automaticReconnectionMs);
        instance.connect();
    }

    public static CustomWebSocketClient getInstance() {
        if (instance == null) {
            throw new NotInitializedException("Please, run first `initialize` method before using this class!");
        }
        return instance;
    }

    @Override
    public void onOpen() {
        Log.i("WebSocket", "Session is starting");
    }

    @Override
    public void onTextReceived(String s) {
        Log.i("WebSocket", "Message received: " + s);

        ServerMessage serverMessage = new ServerMessage(s);
        ServerMessageType serverMessageType = serverMessage.getType();

        if (serverMessageType == null) {
            Log.e("ReceivedMessage", "The server sent us a message of an unknown type: " + serverMessage.getType() +
                    ". Maybe it's valid, but you forgot to add it to the reactions list in the MainActivity.");
            return;
        }

        Class<? extends UserReaction> reactionClass = clientReactionsClasses.get(serverMessageType);
        UserReaction reactionInstance = (UserReaction) Reflection.createInstanceOfClass(reactionClass, serverMessage);

        UserMessage userMessage = null;
        switch (serverMessage.getResult()) {
            case OK:
                userMessage = reactionInstance.react();
                break;
            case FAILURE:
                try {
                    userMessage = reactionInstance.onFailure(serverMessage.toErrorResponse());
                } catch (JsonParserException e) {
                    throw new RuntimeException(e);
                }
                break;
            case ERROR:
                try {
                    userMessage = reactionInstance.onError(serverMessage.toErrorResponse());
                } catch (JsonParserException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new RuntimeException("Server message has result `" + serverMessage.getResult() + "` that has not been handled!");
        }

        if (userMessage != null) {
            this.send(userMessage);
        }
    }

    @Override
    public void onBinaryReceived(byte[] data) {
    }

    @Override
    public void onPingReceived(byte[] data) {
    }

    @Override
    public void onPongReceived(byte[] data) {
    }

    @Override
    public void onException(Exception e) {
        Log.e("Error on message received", e.getMessage());
        e.printStackTrace();
    }

    @Override
    public void onCloseReceived() {
        Log.i("WebSocket", "Closed ");
        System.out.println("onCloseReceived");
    }
    
    public void send(UserMessage userMessage) {
        String json = this.gson.toJson(userMessage);
        this.send(json);
    }
}
