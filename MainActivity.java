package com.techwin.app;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private EditText editTextMultipliers;
    private Button btnUploadScreenshot;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processImageFromUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Elements වල ID පරීක්ෂා කරගන්න
        editTextMultipliers = findViewById(R.id.editTextMultipliers);
        btnUploadScreenshot = findViewById(R.id.btnUploadScreenshot);

        if (btnUploadScreenshot != null) {
            btnUploadScreenshot.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        }
    }

    private void processImageFromUri(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        List<String> multipliers = new ArrayList<>();
                        // Decimal අංක (උදා: 1.12, 2.45, 10.00) සොයන Pattern එක
                        Pattern decimalPattern = Pattern.compile("\\b\\d+\\.\\d+\\b");

                        for (Text.TextBlock block : visionText.getTextBlocks()) {
                            Matcher matcher = decimalPattern.matcher(block.getText());
                            while (matcher.find()) {
                                multipliers.add(matcher.group());
                            }
                        }

                        if (!multipliers.isEmpty()) {
                            String result = String.join(" ", multipliers);
                            if (editTextMultipliers != null) {
                                editTextMultipliers.setText(result);
                            }
                            Toast.makeText(this, "Multipliers extracted!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "No multipliers found in image.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
