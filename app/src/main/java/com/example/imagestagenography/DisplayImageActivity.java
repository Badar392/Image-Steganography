package com.example.imagestagenography;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DisplayImageActivity extends AppCompatActivity {

    private static final String TAG = "DisplayImageActivity";
    private Bitmap stegoImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_image);

        ImageView imageView = findViewById(R.id.imageViewDisplay);
        Button saveButton = findViewById(R.id.btn_save_image);

        // Retrieve the stego image from the intent
        byte[] byteArray = getIntent().getByteArrayExtra("stegoImage");
        if (byteArray != null) {
            stegoImage = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageView.setImageBitmap(stegoImage);
        }

        // Save button functionality
        saveButton.setOnClickListener(v -> {
            if (stegoImage != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveImageToGallery(stegoImage);  // Use MediaStore for Android 10+
                } else {
                    saveImageToInternalStorage(stegoImage);  // Use internal storage for Android 9 and below
                }
            } else {
                Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Save image to MediaStore for Android 10+
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void saveImageToGallery(Bitmap bitmap) {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "stego_image_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StegoImages");

        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (OutputStream outStream = resolver.openOutputStream(uri)) {
                if (outStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                    Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Failed to open output stream");
                    Toast.makeText(this, "Failed to open output stream", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to save image", e);
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Failed to create MediaStore entry");
            Toast.makeText(this, "Failed to create MediaStore entry", Toast.LENGTH_SHORT).show();
        }
    }

    // Save image to internal storage for Android 9 and below
    private void saveImageToInternalStorage(Bitmap bitmap) {
        File directory = new File(getExternalFilesDir(null), "StegoImages");
        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(TAG, "Failed to create directory");
            Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(directory, "stego_image_" + System.currentTimeMillis() + ".png");
        try (FileOutputStream outStream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save image", e);
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    // Embed AES-encrypted message into the image
    public static Bitmap embedMessage(Bitmap image, String message, String key) throws Exception {
        // Encrypt the message using AES
        String encryptedMessage = encryptMessage(message, key);

        // Convert the encrypted message to a binary string
        String binaryMessage = toBinary(encryptedMessage);
        int messageLength = binaryMessage.length();

        // Get the dimensions of the image
        int width = image.getWidth();
        int height = image.getHeight();

        // Ensure the message can fit in the image
        if (messageLength > (width * height * 3)) {
            throw new IllegalArgumentException("Message is too large to fit in the image.");
        }

        // Create a mutable copy of the image
        Bitmap stegoImage = image.copy(Bitmap.Config.ARGB_8888, true);

        int messageIndex = 0;
        for (int y = 0; y < height && messageIndex < messageLength; y++) {
            for (int x = 0; x < width && messageIndex < messageLength; x++) {
                int pixel = image.getPixel(x, y);

                // Extract and modify color channels
                int red = setLSB((pixel >> 16) & 0xFF, binaryMessage.charAt(messageIndex++));
                int green = (messageIndex < messageLength) ? setLSB((pixel >> 8) & 0xFF, binaryMessage.charAt(messageIndex++)) : (pixel >> 8) & 0xFF;
                int blue = (messageIndex < messageLength) ? setLSB(pixel & 0xFF, binaryMessage.charAt(messageIndex++)) : pixel & 0xFF;

                int newPixel = (0xFF << 24) | (red << 16) | (green << 8) | blue;
                stegoImage.setPixel(x, y, newPixel);
            }
        }

        return stegoImage;
    }

    // AES encryption method
    private static String encryptMessage(String message, String key) throws Exception {
        key = enforceKeyLength(key);

        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        byte[] encryptedMessage = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        byte[] encryptedMessageWithIv = new byte[iv.length + encryptedMessage.length];

        System.arraycopy(iv, 0, encryptedMessageWithIv, 0, iv.length);
        System.arraycopy(encryptedMessage, 0, encryptedMessageWithIv, iv.length, encryptedMessage.length);

        return Base64.encodeToString(encryptedMessageWithIv, Base64.DEFAULT);
    }

    private static String enforceKeyLength(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return new String(Arrays.copyOf(keyBytes, 16), StandardCharsets.UTF_8);
    }

    // Convert a string to binary
    private static String toBinary(String message) {
        StringBuilder binary = new StringBuilder();
        for (char c : message.toCharArray()) {
            binary.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
        }
        return binary.toString();
    }

    // Set the least significant bit
    private static int setLSB(int color, char bit) {
        return (color & 0xFE) | (bit == '1' ?1:0);
}
}