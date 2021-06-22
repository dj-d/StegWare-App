package com.android.stegware_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ayush.imagesteganographylibrary.Text.AsyncTaskCallback.TextDecodingCallback;
import com.ayush.imagesteganographylibrary.Text.ImageSteganography;
import com.ayush.imagesteganographylibrary.Text.TextDecoding;

import java.io.IOException;

public class Decode extends AppCompatActivity implements TextDecodingCallback {

    private static final int SELECT_PICTURE = 100;
    private static final String TAG = "DecodeClass";

    // Initializing the UI components
    private TextView textView;
    private ImageView imageView;
    private EditText message;
    private Uri filepath;

    // Bitmap
    private Bitmap original_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);

        // Instantiation of UI components
        textView = findViewById(R.id.whether_decoded);

        imageView = findViewById(R.id.imageview);

        message = findViewById(R.id.message);

        Button choose_image_button = findViewById(R.id.choose_image_button);
        Button decode_button = findViewById(R.id.decode_button);

        // Choose Image Button
        choose_image_button.setOnClickListener(view -> ImageChooser());

        // Decode Button
        decode_button.setOnClickListener(view -> {
            if (filepath != null) {

                // Making the ImageSteganography object
                ImageSteganography imageSteganography = new ImageSteganography("a", original_image);

                // Making the TextDecoding object
                TextDecoding textDecoding = new TextDecoding(Decode.this, Decode.this);

                // Execute Task
                textDecoding.execute(imageSteganography);
            }
        });
    }

    private void ImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Image set to imageView
        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {

            filepath = data.getData();

            try {
                original_image = MediaStore.Images.Media.getBitmap(getContentResolver(), filepath);

                imageView.setImageBitmap(original_image);
            } catch (IOException e) {
                Log.d(TAG, "Error : " + e);
            }
        }
    }

    @Override
    public void onStartTextEncoding() {
        // Whatever you want to do by the start of textDecoding
    }

    @Override
    public void onCompleteTextEncoding(ImageSteganography result) {

        // By the end of textDecoding

        if (result != null) {
            if (!result.isDecoded())
                textView.setText("No message found");
            else {
                if (!result.isSecretKeyWrong()) {
                    textView.setText("Decoded");
                    message.setText("Res: " + result.getMessage());

                    Log.d(TAG, "res: " + result.getMessage());
                } else {
                    textView.setText("Wrong secret key");
                }
            }
        } else {
            textView.setText("Select Image First");
        }
    }
}
