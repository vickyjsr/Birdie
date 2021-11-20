package com.example.chatapp.adapters;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.databinding.ItemContainerRecentConversionBinding;
import com.example.chatapp.listeners.ConversationListener;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.Users;

import java.security.MessageDigest;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConversionViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final ConversationListener conversationListener;

    public RecentConversationsAdapter(List<ChatMessage> chatMessages, ConversationListener conversationListener) {
        this.chatMessages = chatMessages;
        this.conversationListener = conversationListener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),parent,
                        false));
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversionBinding binding;
        private final String AES = "AES";

        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding)
        {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(ChatMessage chatMessage) {

            Glide.with(binding.imageProfile.getContext()).load((chatMessage.conversationImage)).into(binding.imageProfile);
            binding.txtname.setText(chatMessage.conversationName);
            binding.textRecentMessage.setText(chatMessage.message);

            binding.getRoot().setOnClickListener(v-> {
                Users users = new Users();
                users.id = chatMessage.conversationId;
                users.name = chatMessage.conversationName;
                users.image = chatMessage.conversationImage;
                conversationListener.onConversationClicked(users);
            });
        }

        private String decrypt(String password, String s) throws Exception {
            Log.d("decrypt", "decrypt: inside");
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
