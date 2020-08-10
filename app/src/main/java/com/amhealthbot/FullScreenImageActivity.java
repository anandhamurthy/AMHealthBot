package com.amhealthbot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.amhealthbot.Adapters.ChatAdapter;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        Intent intent = getIntent();
        String image = intent.getStringExtra("image");

        PhotoView photoView = (PhotoView) findViewById(R.id.image);
        Glide.with(FullScreenImageActivity.this).load(image).into(photoView);
    }
}