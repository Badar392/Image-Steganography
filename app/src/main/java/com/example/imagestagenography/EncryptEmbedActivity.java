package com.example.imagestagenography;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Objects;

public class EncryptEmbedActivity extends AppCompatActivity {

    private static final String TAG = "EncryptEmbedActivity"; // Logging Tag
    private ImageView imageView;
    private EditText etMessage, etKey;
    private Bitmap selectedImage;

    // Define the ActivityResultLauncher for picking an image
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    InputStream inputStream = null;
                    try {
                        inputStream = getContentResolver().openInputStream(Objects.requireNonNull(result.getData().getData()));
                        selectedImage = BitmapFactory.decodeStream(inputStream);
                        imageView.setImageBitmap(selectedImage);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading image: " + e.getMessage(), e);
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close(); // Properly close the stream
                            } catch (Exception e) {
                                Log.e(TAG, "Error closing stream: " + e.getMessage(), e);
                            }
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypt_embed);

        imageView = findViewById(R.id.imageView);
        etMessage = findViewById(R.id.et_message);
        etKey = findViewById(R.id.et_key);

        Button btnSelectImage = findViewById(R.id.btn_select_image);
        Button btnEmbedMessage = findViewById(R.id.btn_embed_message);

        btnSelectImage.setOnClickListener(v -> selectImage());
        btnEmbedMessage.setOnClickListener(v -> embedMessage());
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void embedMessage() {
        try {
            String message = etMessage.getText().toString();
            String key = etKey.getText().toString();

            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter a key", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedImage == null) {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
                return;
            }

            // Embed the AES-encrypted message into the image
            Bitmap stegoImage = DisplayImageActivity.embedMessage(selectedImage, message, key);

            // Save the stego image, message, and key temporarily using SharedPreferences
            saveDataToPreferences(stegoImage, message, key);

            // Pass the stego image to DisplayImageActivity
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stegoImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            Intent intent = new Intent(EncryptEmbedActivity.this, DisplayImageActivity.class);
            intent.putExtra("stegoImage", byteArray);
            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error embedding message: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to embed message", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDataToPreferences(Bitmap stegoImage, String message, String key) {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("StegoData", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            // Convert Bitmap to a byte array for storage
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stegoImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            String imageString = Base64.encodeToString(byteArray, Base64.DEFAULT);

            // Store the stego image, message, and key in SharedPreferences
            editor.putString("stego_image", imageString);
            editor.putString("stego_message", message);
            editor.putString("encryption_key", key);

            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving data to preferences: " + e.getMessage(), e);
        }
    }
}