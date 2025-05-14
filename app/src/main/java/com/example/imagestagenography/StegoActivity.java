package com.example.imagestagenography;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class StegoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stego);

        // Initializing buttons
        Button btnEmbed = findViewById(R.id.btn_embed);
        Button btnExtract = findViewById(R.id.btn_extract);

        // Set click listener for Embed button
        btnEmbed.setOnClickListener(v -> {
            // Navigate to Encrypt/Embed Activity
            Intent intent = new Intent(StegoActivity.this, EncryptEmbedActivity.class);
            startActivity(intent);
        });

        // Set click listener for Extract button
        btnExtract.setOnClickListener(v -> {
            // Navigate to Decrypt/Extract Activity
            Intent intent = new Intent(StegoActivity.this, DecryptExtractActivity.class);
            startActivity(intent);
        });
    }
}