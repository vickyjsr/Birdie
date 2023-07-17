package com.example.chatapp.utilities;

import java.util.HashMap;

public class Constants {
    public static final String KEY_COLLECTION_USERS = "users";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_PREFERENCE_NAME = "chatappPreferences";
    public static final String KEY_PREFERENCE_IS_FIRST_TIME = "onborading";
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_FCM_TOKEN = "fcmtoken";
    public static final String KEY_USER = "user";
    public static final String KEY_COLLECTION_CHAT = "chat";
    public static final String KEY_SENDER_ID = "senderid";
    public static final String KEY_RECEIVER_ID = "recieverid";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_COLLECTION_CONVERSATIONS = "conversations";
    public static final String KEY_RECEIVER_NAME = "recieverName";
    public static final String KEY_SENDER_NAME = "senderName";
    public static final String KEY_SENDER_IMAGE = "senderImage";
    public static final String KEY_RECEIVER_IMAGE = "recieverImage";
    public static final String KEY_LAST_MESSAGE = "lastMessage";
    public static final String KEY_AVAILABILITY = "online";
    public static final String REMOTE_MSG_AUTHORIZATION = "Authorization";
    public static final String REMOTE_MSG_CONTENT_TYPE = "Content-Type";
    public static final String REMOTE_MSG_DATA = "data";
    public static final String REMOTE_MSG_REGISTRATION_IDS = "registration_ids";
    public static final String CHAT_UNIQUE_ID = "uniqueID";
    public static final String RECEIVED_MESSAGE_EMOJI = "emoji";

    public static HashMap<String, String> remoteMsgHeaders = null;

    public static HashMap<String, String> getRemoteMsgHeaders() {
        remoteMsgHeaders = new HashMap<>();
        remoteMsgHeaders.put(REMOTE_MSG_AUTHORIZATION, "key=AAAAILmrj6Y:APA91bHZpRiV_5PRLEVhKwzwIiHzNTwE9oskP7Beq5BvJ0ACYkb9FR__jnW497blV35LG_3989Y5aeUInNkvBRWslgnKuEutzEDa5Kp-WrdvpSUMzcRDX1D3R3FRS7Q1r5D7QLe8nNEh");
        remoteMsgHeaders.put(REMOTE_MSG_CONTENT_TYPE, "application/json");
        return remoteMsgHeaders;
    }
}
