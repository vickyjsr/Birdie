package com.example.chatapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.models.Users;
import com.example.chatapp.utilities.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class Receiver_info extends AppCompatActivity {

    private Users recieveUsers;
    private ImageView profileImage;
    private TextView name, online;
    private FirebaseFirestore database;
    private boolean isReceiverOnline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reciever_info);

        recieveUsers = (Users) getIntent().getSerializableExtra(Constants.KEY_USER);
        database = FirebaseFirestore.getInstance();

        profileImage = findViewById(R.id.rProfile_image);
        name = findViewById(R.id.textname);
        online = findViewById(R.id.textOnline);

        Glide.with(this).load(Uri.parse(recieveUsers.image)).into(profileImage);

        name.setText(recieveUsers.name);

        database.collection(Constants.KEY_COLLECTION_USERS).document(recieveUsers.id).addSnapshotListener(this, ((value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    int online = Objects.requireNonNull(value.getLong(Constants.KEY_AVAILABILITY)).intValue();
                    isReceiverOnline = online == 1;
                }
                recieveUsers.token = value.getString(Constants.KEY_FCM_TOKEN);
            }
            if (isReceiverOnline) {
                online.setText("Online");
            } else {
                online.setText("Offline");
            }
        }));
    }
}