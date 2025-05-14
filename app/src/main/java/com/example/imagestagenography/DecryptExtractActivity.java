package com.example.imagestagenography;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DecryptExtractActivity extends AppCompatActivity {

    private ImageView imageViewStego;
    private EditText editTextDecryptionKey;
    private TextView textViewSecretMessage;
    private Bitmap selectedImage;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try (InputStream imageStream = getContentResolver().openInputStream(imageUri)) {
                            selectedImage = BitmapFactory.decodeStream(imageStream);
                            imageViewStego.setImageBitmap(selectedImage);
                        } catch (IOException e) {
                            showToast("Error loading image. Please try again.");
                        }
                    } else {
                        showToast("Invalid image selection.");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypt_extract);

        imageViewStego = findViewById(R.id.imageViewStego);
        editTextDecryptionKey = findViewById(R.id.editTextDecryptionKey);
        textViewSecretMessage = findViewById(R.id.textViewSecretMessage);
        Button btnSelectStegoImage = findViewById(R.id.btn_select_stego_image);
        Button btnExtractMessage = findViewById(R.id.btn_extract_message);

        btnSelectStegoImage.setOnClickListener(v -> selectImage());
        btnExtractMessage.setOnClickListener(v -> extractMessage(editTextDecryptionKey.getText().toString().trim()));
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void extractMessage(String userInputKey) {
        if (selectedImage == null) {
            showToast("Please select a stego image first.");
            return;
        }
        if (userInputKey.isEmpty()) {
            showToast("Please enter the decryption key.");
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("StegoData", MODE_PRIVATE);
        String storedImageBase64 = sharedPreferences.getString("stego_image", null);
        String storedMessage = sharedPreferences.getString("stego_message", null);
        String storedEncryptionKey = sharedPreferences.getString("encryption_key", null);

        if (storedImageBase64 == null || storedMessage == null || storedEncryptionKey == null) {
            showToast("No stego data found.");
            return;
        }

        Bitmap storedImage = decodeBase64ToBitmap(storedImageBase64);
        if (storedImage == null) {
            showToast("Stored image is corrupted.");
            return;
        }

        if (!compareBitmaps(selectedImage, storedImage)) {
            showToast("The selected image does not match the stego image.");
            return;
        }

        if (!userInputKey.equals(storedEncryptionKey)) {
            showToast("Incorrect decryption key.");
            return;
        }

        textViewSecretMessage.setText(storedMessage);
    }

    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeStream(new ByteArrayInputStream(decodedBytes));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean compareBitmaps(Bitmap bitmap1, Bitmap bitmap2) {
        return bitmap1.getWidth() == bitmap2.getWidth()
                && bitmap1.getHeight() == bitmap2.getHeight()
                && bitmap1.sameAs(bitmap2);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}