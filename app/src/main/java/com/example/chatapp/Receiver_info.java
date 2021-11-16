package com.example.chatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;

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

        profileImage.setImageBitmap(getBitmapFromEncodedString(recieveUsers.image));
        name.setText(recieveUsers.name);

        database.collection(Constants.KEY_COLLECTION_USERS).document(
                recieveUsers.id).addSnapshotListener(this, ((value, error) -> {
            if(error!=null) {
                return;
            }
            if(value!=null) {
                if(value.getLong(Constants.KEY_AVAILABILITY)!=null)
                {
                    int online = Objects.requireNonNull(value.getLong(Constants.KEY_AVAILABILITY)).intValue();
                    isReceiverOnline = online==1;
                }
                recieveUsers.token = value.getString(Constants.KEY_FCM_TOKEN);
            }
            if(isReceiverOnline) {
                online.setText("Online");
            }
            else {
                online.setText("Offline");
            }
        }));
    }
    private Bitmap getBitmapFromEncodedString(String encodedimage)
    {
        if(encodedimage!=null)
        {
            byte[] bytes = Base64.decode(encodedimage,Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }
        else {
            return null;
        }
    }
}