package com.example.chatapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.chatapp.R;
import com.example.chatapp.adapters.ChatAdapter;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.example.chatapp.listeners.SingleChatRemove;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.Users;
import com.example.chatapp.network.ApiClient;
import com.example.chatapp.network.ApiService;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ChatActivity extends BaseActivity implements SingleChatRemove {

    private ActivityChatBinding binding;
    private Users receiveUsers;
    private List<ChatMessage> chatMessages;
    private PreferenceManager preferenceManager;
    private ChatAdapter chatAdapter;
    private FirebaseFirestore database;
    private String conversationId = null;
    private Boolean isReceiverOnline = false;
    private final String AES = "AES";
    private ValueEventListener seenListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
    }

    private void init()
    {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, Uri.parse(receiveUsers.image),preferenceManager.getString(Constants.KEY_USER_ID), this, this);
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiveUsers.id);
        message.put(Constants.KEY_TIMESTAMP, new Date());

        String s = null;

        try {
            s = getUniqueId();
        } catch (Exception e) {
            e.printStackTrace();
        }

        s += Calendar.getInstance().getTimeInMillis() + "";

        message.put(Constants.CHAT_UNIQUE_ID,s);
        message.put(Constants.RECEIVED_MESSAGE_EMOJI,"0");

        String checkSpace = binding.inputMessage.getText().toString();

        if(checkSpace.trim().isEmpty())return;

        String encrypted = null;
        try {
            encrypted = encrypt(s, checkSpace);
        } catch (Exception e) {
            e.printStackTrace();
        }

        message.put(Constants.KEY_MESSAGE,encrypted);

        message.put(Constants.KEY_TIMESTAMP,new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        if(conversationId!=null)
        {
            updateConversation(checkSpace);
        }
        else {
            HashMap<String, Object> conversation = new HashMap<>();
            conversation.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
            conversation.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversation.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversation.put(Constants.KEY_RECEIVER_ID, receiveUsers.id);
            conversation.put(Constants.KEY_RECEIVER_NAME, receiveUsers.name);
            conversation.put(Constants.KEY_RECEIVER_IMAGE, receiveUsers.image);
            conversation.put(Constants.KEY_LAST_MESSAGE, checkSpace);
            conversation.put(Constants.CHAT_UNIQUE_ID,s);
            addConversation(conversation);
        }

        MediaPlayer mp = MediaPlayer.create(this, R.raw.short_sms_tone);
        mp.setOnCompletionListener(mp1 -> {
            mp1.pause();
            mp1.stop();
        });
        mp.start();

        if(!isReceiverOnline) {
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiveUsers.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
                data.put(Constants.KEY_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                sendNotification(body.toString());
            }
            catch (Exception e) {
                showToast(e.getMessage());
            }
        }
        binding.inputMessage.setText(null);
    }

    private String encrypt(String password, String s) throws Exception {
        SecretKeySpec keySpec = generateKey(password);
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.ENCRYPT_MODE,keySpec);
        byte[] encVal = c.doFinal(s.getBytes());
        return Base64.encodeToString(encVal,Base64.DEFAULT);
    }

    private SecretKeySpec generateKey(String password) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = password.getBytes("UTF-8");
        digest.update(bytes,0,bytes.length);
        byte[] key = digest.digest();
        return new SecretKeySpec(key,AES);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messagebody) {
        ApiClient.getClient().create(ApiService.class).sendMessage(Constants.getRemoteMsgHeaders()
        , messagebody).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if(response.isSuccessful()) {
                    try {
                        if(response.body()!=null) {
                            JSONObject responseJSON = new JSONObject(response.body());
                            JSONArray results = responseJSON.getJSONArray("results");
                            if(responseJSON.getInt("failure")==1) {
                                JSONObject error = (JSONObject) results.get(0);
                            }
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
//                    showToast("Notification sent Successfully");
                }
                else {
                    showToast("Error: "+response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void seenMessages(String uid) {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_USER_ID, uid)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful() && !Objects.requireNonNull(task.getResult()).isEmpty()) {
                    DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                    String documentID = documentSnapshot.getId();

//                    database.collection(Constants.KEY_COLLECTION_CHAT)
//                            .document(documentID)
//                            .update(Constants.RECEIVED_MESSAGE_EMOJI,emoji)

                }
            }
        });
    }

    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiveUsers.id).addSnapshotListener(ChatActivity.this, ((value, error) -> {
                    if(error!=null) {
                        return;
                    }
                    if(value!=null) {
                        if(value.getLong(Constants.KEY_AVAILABILITY)!=null)
                        {
                            int online = Objects.requireNonNull(value.getLong(Constants.KEY_AVAILABILITY)).intValue();
                            isReceiverOnline = online==1;
                        }
                        receiveUsers.token = value.getString(Constants.KEY_FCM_TOKEN);
                        if(receiveUsers.image==null) {
                            receiveUsers.image = value.getString(Constants.KEY_IMAGE);
                            chatAdapter.setReceiveProfileImage(Uri.parse(receiveUsers.image));
                            chatAdapter.notifyItemRangeChanged(0,chatMessages.size());
                        }
                    }
            if(isReceiverOnline) {
                binding.textOnline.setVisibility(View.VISIBLE);
            }
            else {
                binding.textOnline.setVisibility(View.GONE);
            }

        }));
    }

    public void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiveUsers.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiveUsers.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }



    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error)->{
        if(error!=null)
        {
            return;
        }
        if(value!=null)
        {
            int count = chatMessages.size();
            for(DocumentChange documentChange:value.getDocumentChanges())
            {
                if(documentChange.getType()==DocumentChange.Type.ADDED)
                {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.recieverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateobject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.uniqueID = documentChange.getDocument().getString(Constants.CHAT_UNIQUE_ID);
                    chatMessage.emojiReciever = documentChange.getDocument().getString(Constants.RECEIVED_MESSAGE_EMOJI);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, Comparator.comparing(obj -> obj.dateobject));
            if(count==0)
            {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeChanged(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size()-1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressbar.setVisibility(View.GONE);
        if(conversationId==null)
        {
            checkForConversation();
        }
    };

    public static String getUniqueId() {
        return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
    }

    private void loadReceiverDetails() {
        receiveUsers = (Users) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textname.setText(receiveUsers.name);
    }

    private static String getReadableDateTime(Date date)
    {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversation(HashMap<String, Object> conversation) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    private void updateConversation(String message) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date());
    }

    private void checkForConversation() {
        if(chatMessages.size()!=0)
        {
            checkForConversationRemotely(preferenceManager.getString(Constants.KEY_USER_ID),
                    receiveUsers.id);

            checkForConversationRemotely(receiveUsers.id,
                    preferenceManager.getString(Constants.KEY_USER_ID));
        }
    }

    private void checkForConversationRemotely(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = new OnCompleteListener<QuerySnapshot>() {
        @Override
        public void onComplete(@NonNull Task<QuerySnapshot> task) {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                conversationId = documentSnapshot.getId();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void removeItemAt(int position) {
        String encrypt = null;
        try {
            encrypt = encrypt(chatMessages.get(position).uniqueID,"This Message was Deleted!!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        chatMessages.get(position).message = encrypt;
        chatAdapter.notifyDataSetChanged();

        String decrypted = null;
        try {
            decrypted = decrypt(chatMessages.get(position).uniqueID,chatMessages.get(position).message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateConversation(decrypted);
    }

    private String decrypt(String password, String s) throws Exception {
        SecretKeySpec keySpec = generateKey(password);
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.DECRYPT_MODE,keySpec);
        byte[] decodedValue = Base64.decode(s,Base64.DEFAULT);
        byte[] decVal = c.doFinal(decodedValue);
        return new String(decVal);
    }

    private void setListeners() {
        binding.imageback.setOnClickListener(v-> onBackPressed());
        binding.layoutSend.setOnClickListener(v->sendMessage());
        binding.info.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Receiver_info.class);
            intent.putExtra(Constants.KEY_USER, receiveUsers);
            startActivity(intent);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void reactionListener(int position, String emoji) {
        chatMessages.get(position).emojiReciever = emoji;
        chatAdapter.notifyDataSetChanged();
//        if anything happen remove below line
        binding.chatRecyclerView.smoothScrollToPosition(position);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}