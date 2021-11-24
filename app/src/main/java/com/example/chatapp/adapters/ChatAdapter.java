package com.example.chatapp.adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.databinding.ItemContainerRecievedMessageBinding;
import com.example.chatapp.databinding.ItemContainerSentMessageBinding;
import com.example.chatapp.listeners.SingleChatRemove;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.utilities.Constants;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private Uri receiveProfileImage;
    private final String senderId;
    private final FirebaseFirestore database = FirebaseFirestore.getInstance();
    private final Context context;
    private final SingleChatRemove singleChatRemove;

    public void setReceiveProfileImage(Uri uri) {
        receiveProfileImage = uri;
    }

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;
    private final String AES = "AES";

    public ChatAdapter(List<ChatMessage> chatMessages, Uri receiveProfileImage, String senderId, Context context, SingleChatRemove singleChatRemove) {
        this.chatMessages = chatMessages;
        this.receiveProfileImage = receiveProfileImage;
        this.senderId = senderId;
        this.context = context;
        this.singleChatRemove = singleChatRemove;
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

    public void delete_A_Chat(int position, int type) {

        if(type== VIEW_TYPE_RECEIVED)return;

        ChatMessage message = chatMessages.get(position);
        String encrypt = null;
        try {
            encrypt = encrypt(message.uniqueID,"This Message was Deleted!!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        String finalEncrypt = encrypt;
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.CHAT_UNIQUE_ID,message.uniqueID)
                .whereEqualTo(Constants.KEY_MESSAGE, message.message)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !Objects.requireNonNull(task.getResult()).isEmpty()) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        String documentID = documentSnapshot.getId();

                        database.collection(Constants.KEY_COLLECTION_CHAT)
                                .document(documentID)
                                .update(Constants.KEY_MESSAGE, finalEncrypt)
                                .addOnSuccessListener(aVoid -> {
                                    singleChatRemove.removeItemAt(position);
//                                    Toast.makeText(context, "Deleted!!", Toast.LENGTH_SHORT).show();
                                }).addOnFailureListener(e -> {});
                    }
                });
    }




    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==VIEW_TYPE_SENT)
        {
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(LayoutInflater.from(parent.getContext()),
                            parent,false));
        }
        else {
            return new ReceivedMessageViewHolder(
                    ItemContainerRecievedMessageBinding.inflate(LayoutInflater.from(parent.getContext()),
                            parent,false));
        }



    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        holder.setIsRecyclable(false);
        if(getItemViewType(position)==VIEW_TYPE_SENT)
        {
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));

            String dec = null;
            try {
                dec = ((SentMessageViewHolder) holder).decrypt(chatMessages.get(position).uniqueID,chatMessages.get(position).message);
            } catch (Exception e) {
                e.printStackTrace();
            }

            assert dec != null;

            if(!dec.equals("This Message was Deleted!!")) {
                holder.itemView.setOnClickListener(v -> {
                    AlertDialog.Builder builder =new AlertDialog.Builder(context);
                    builder.setMessage("Sure you want to delete?").setCancelable(false).setPositiveButton("YES", (dialog, which) -> {
                        delete_A_Chat(position, getItemViewType(position));
                        ((SentMessageViewHolder) holder).binding.textmessage.setBackgroundResource(R.drawable.background_sent_message_deleted);
                    })
                            .setNegativeButton("No", (dialog, which) -> dialog.cancel());
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                });
            }
        }
        else {
            ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position), receiveProfileImage);

            String dec = null;
            try {
                dec = ((ReceivedMessageViewHolder) holder).decrypt(chatMessages.get(position).uniqueID,chatMessages.get(position).message);
            } catch (Exception e) {
                e.printStackTrace();
            }

            assert dec != null;

            if(!dec.equals("This Message was Deleted!!")) {
                holder.itemView.setOnClickListener(v -> react(position, getItemViewType(position)));
            }
        }

    }

    private void react(int position, int itemViewType) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.custom_dialog_reaction);
        ImageView close = dialog.findViewById(R.id.iv_add);
        ImageView like = dialog.findViewById(R.id.iv_like);
        ImageView heart = dialog.findViewById(R.id.iv_heart);
        ImageView wow = dialog.findViewById(R.id.iv_wow);
        ImageView haha = dialog.findViewById(R.id.iv_haha);
        ImageView angry = dialog.findViewById(R.id.iv_angry);
        ImageView sad = dialog.findViewById(R.id.iv_sad);

        close.setOnClickListener(v -> dialog.dismiss());

        like.setOnClickListener(v -> {
            uploadEmoji(position, "0x1F44D");
            dialog.dismiss();
        });
        heart.setOnClickListener(v -> {
            uploadEmoji(position, "0x1F499");
            dialog.dismiss();
        });
        wow.setOnClickListener(v -> {
            uploadEmoji(position, "0x1F62E");
            dialog.dismiss();
        });
        haha.setOnClickListener(v -> {
            uploadEmoji(position, "0x1F606");
            dialog.dismiss();
        });
        angry.setOnClickListener(v -> {
            uploadEmoji(position, "0x1F621");
            dialog.dismiss();
        });
        sad.setOnClickListener(v -> {
            uploadEmoji(position, "0x1F625");
            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void uploadEmoji(int position,String emoji) {
        ChatMessage message = chatMessages.get(position);

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.CHAT_UNIQUE_ID,message.uniqueID)
                .whereEqualTo(Constants.RECEIVED_MESSAGE_EMOJI, message.emojiReciever)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !Objects.requireNonNull(task.getResult()).isEmpty()) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        String documentID = documentSnapshot.getId();

                        database.collection(Constants.KEY_COLLECTION_CHAT)
                                .document(documentID)
                                .update(Constants.RECEIVED_MESSAGE_EMOJI,emoji)
                                .addOnSuccessListener(aVoid -> {
                                    singleChatRemove.reactionListener(position,emoji);
//                                    Toast.makeText(context, "Reacted!!", Toast.LENGTH_SHORT).show();
                                }).addOnFailureListener(e -> {

                                });
                    }
                });
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatMessages.get(position).senderId.equals(senderId))
        {
            return VIEW_TYPE_SENT;
        }
        else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerSentMessageBinding binding;
        private final String AES = "AES";

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding)
        {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage)
        {
            String decrypted = null;
            try {
                decrypted = decrypt(chatMessage.uniqueID,chatMessage.message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            binding.textmessage.setText(decrypted);
            binding.textdateTime.setText(chatMessage.dateTime);
            String s = chatMessage.emojiReciever;

            assert decrypted != null;
            if(decrypted.equals("This Message was Deleted!!")) {
                binding.textmessage.setBackgroundResource(R.drawable.background_sent_message_deleted);
                binding.textmessage.setTextSize(14);
            }

            int x = 0;
            switch (s) {
                case "0x1F44D":
                    x = 0x1F44D; // like
                    break;
                case "0x1F499":
                    x = 0x1F499; // heart
                    break;
                case "0x1F606":
                    x = 0x1F606; // haha
                    break;
                case "0x1F625":
                    x = 0x1F625; // sad
                    break;
                case "0x1F62E":
                    x = 0x1F62E; // wow
                    break;
                case "0x1F621":
                    x = 0x1F621; // angry
                    break;
            }

            if(!decrypted.equals("This Message was Deleted!!"))
            binding.reaction.setText(getUnicodeToEmoji(x));
        }

        private String getUnicodeToEmoji(int unicode) {
            return new String(Character.toChars(unicode));
        }

        private String decrypt(String password, String s) throws Exception {
            SecretKeySpec keySpec = generateKey(password);
            Cipher c = Cipher.getInstance(AES);
            c.init(Cipher.DECRYPT_MODE,keySpec);
            byte[] decodedValue = Base64.decode(s,Base64.DEFAULT);
            byte[] decVal = c.doFinal(decodedValue);
            return new String(decVal);
        }

        private SecretKeySpec generateKey(String password) throws Exception {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = password.getBytes("UTF-8");
            digest.update(bytes,0,bytes.length);
            byte[] key = digest.digest();
            return new SecretKeySpec(key,AES);
        }

    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerRecievedMessageBinding binding;
        private final String AES = "AES";

        ReceivedMessageViewHolder(ItemContainerRecievedMessageBinding itemContainerRecievedMessageBinding)
        {
            super(itemContainerRecievedMessageBinding.getRoot());
            binding = itemContainerRecievedMessageBinding;
        }

        void setData(ChatMessage chatMessage,Uri receiverProfileImage) {

            String decrypted = null;
            try {
                decrypted = decrypt(chatMessage.uniqueID,chatMessage.message);
            } catch (Exception e) {
                e.printStackTrace();
            }

            binding.textmessage.setText(decrypted);
            binding.textdateTime.setText(chatMessage.dateTime);

            assert decrypted != null;
            if(decrypted.equals("This Message was Deleted!!")) {
                binding.textmessage.setBackgroundResource(R.drawable.background_recieved_message_deleted);
                binding.textmessage.setTextSize(14);
            }

            String s = chatMessage.emojiReciever;
            int x = 0;
            switch (s) {
                case "0x1F44D":
                    x = 0x1F44D; // like
                    break;
                case "0x1F499":
                    x = 0x1F499; // heart
                    break;
                case "0x1F606":
                    x = 0x1F606; // haha
                    break;
                case "0x1F625":
                    x = 0x1F625; // sad
                    break;
                case "0x1F62E":
                    x = 0x1F62E; // wow
                    break;
                case "0x1F621":
                    x = 0x1F621; // angry
                    break;
            }

            if(!decrypted.equals("This Message was Deleted!!"))
                binding.reaction.setText(getUnicodeToEmoji(x));

            if(receiverProfileImage!=null) {
                Glide.with(binding.imageProfile.getContext()).load(receiverProfileImage).into(binding.imageProfile);
            }
        }

        private String getUnicodeToEmoji(int unicode) {
            return new String(Character.toChars(unicode));
        }

        private String decrypt(String password, String s) throws Exception {
            SecretKeySpec keySpec = generateKey(password);
            Cipher c = Cipher.getInstance(AES);
            c.init(Cipher.DECRYPT_MODE,keySpec);
            byte[] decodedValue = Base64.decode(s,Base64.DEFAULT);
            byte[] decVal = c.doFinal(decodedValue);
            return new String(decVal);
        }

        private SecretKeySpec generateKey(String password) throws Exception {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = password.getBytes("UTF-8");
            digest.update(bytes,0,bytes.length);
            byte[] key = digest.digest();
            return new SecretKeySpec(key,AES);
        }

    }




}
