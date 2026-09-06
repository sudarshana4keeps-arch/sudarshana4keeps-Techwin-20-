package com.techwin.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private EditText inputEditText;
    private TextView txtAnalysisResult, txtHighlightedMultipliers;
    private Button btnAnalyze, btnClear, btnUploadImage;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Components
        inputEditText = findViewById(R.id.editTextMultipliers);
        txtAnalysisResult = findViewById(R.id.txtAnalysisResult);
        txtHighlightedMultipliers = findViewById(R.id.txtHighlightedMultipliers);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        btnClear = findViewById(R.id.btnClear);
        btnUploadImage = findViewById(R.id.btnUploadImage);

        // Screenshot එකක් Upload කළ පසු Auto Process වන කොටස
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        processImageWithMLKit(imageUri);
                    }
                }
            }
        );

        btnUploadImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnAnalyze.setOnClickListener(v -> analyzeData());

        btnClear.setOnClickListener(v -> {
            inputEditText.setText("");
            txtAnalysisResult.setText("");
            txtHighlightedMultipliers.setText("");
        });
    }

    // Screenshot එකෙන් Multipliers 100ක් දක්වා ස්වයංක්‍රීයව Extract කරගැනීම
    private void processImageWithMLKit(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<String> multipliers = new ArrayList<>();
                    // Multipliers සොයාගැනීමේ Pattern එක (උදා: 1.00, 2.45x, 10.5)
                    Pattern pattern = Pattern.compile("\\b\\d{1,3}\\.\\d{1,2}x?\\b");
                    
                    for (com.google.mlkit.vision.text.Text.TextBlock block : visionText.getTextBlocks()) {
                        Matcher matcher = pattern.matcher(block.getText());
                        while (matcher.find()) {
                            multipliers.add(matcher.group().replace("x", ""));
                        }
                    }

                    if (!multipliers.isEmpty()) {
                        String result = String.join(" ", multipliers);
                        inputEditText.setText(result);
                        Toast.makeText(this, "Multipliers Auto-Extracted Successfully!", Toast.LENGTH_SHORT).show();
                        analyzeData(); // Auto analyze
                    } else {
                        Toast.makeText(this, "No valid multipliers found in screenshot!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error reading image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // සියලුම අවශ්‍යතාවලට අදාළ විශ්ලේෂණ Algorithm එක
    private void analyzeData() {
        String input = inputEditText.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter or upload multipliers first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] rawList = input.split("[\\s,]+");
        List<Double> values = new ArrayList<>();

        for (String str : rawList) {
            try {
                double val = Double.parseDouble(str.replace("x", ""));
                values.add(val);
            } catch (NumberFormatException ignored) {}
        }

        if (values.size() < 2) {
            Toast.makeText(this, "At least 2 multipliers required for analysis!", Toast.LENGTH_SHORT).show();
            return;
        }

        // පරණ ගිය රවුන්ඩ් 100ක් දක්වා සීමා කිරීම (Limit to last 100 rounds)
        if (values.size() > 100) {
            values = values.subList(values.size() - 100, values.size());
        }

        int totalRounds = values.size();
        int count10x = 0;
        int count2x = 0;
        int lowRounds = 0;
        double sum = 0;

        StringBuilder coloredText = new StringBuilder();

        // Highlighting: 10x+ (Pink), 2x+ (Blue)
        for (double val : values) {
            sum += val;
            if (val >= 10.0) {
                count10x++;
                coloredText.append("<font color='#FF1493'><b>").append(val).append("x </b></font>"); // Pink Color
            } else if (val >= 2.0) {
                count2x++;
                coloredText.append("<font color='#1E90FF'><b>").append(val).append("x </b></font>"); // Blue Color
            } else {
                lowRounds++;
                coloredText.append("<font color='#808080'>").append(val).append("x </font>"); // Gray
            }
        }

        txtHighlightedMultipliers.setText(Html.fromHtml(coloredText.toString(), Html.FROM_HTML_MODE_LEGACY));

        double avgMultiplier = sum / totalRounds;

        // Betting Volume & Platform Profit/Loss Logic Analysis
        String platformState;
        String betVolumeAnalysis;
        String bigWinTiming;

        if (avgMultiplier < 1.85) {
            platformState = "<font color='#00FF00'><b>Platform Profit Zone (High Player Losses)</b></font>";
            betVolumeAnalysis = " High player betting volume detected with low payouts. Platform margin is HIGH.";
            bigWinTiming = " <b>HIGH PROBABILITY OF BIG WIN / PINK (10x+) SOON!</b> (Platform has collected enough margins).";
        } else if (avgMultiplier > 3.2) {
            platformState = "<font color='#FF0000'><b>Platform Loss Zone (High Payout Phase)</b></font>";
            betVolumeAnalysis = " Low platform profit phase. High multipliers given out recently.";
            bigWinTiming = " <b>HIGH RISK ZONE!</b> Betting volume drops/low multipliers (1.00x - 1.30x) expected next.";
        } else {
            platformState = "<font color='#FFFF00'><b>Balanced Platform State</b></font>";
            betVolumeAnalysis = " Standard bet distribution. Moderate platform profit.";
            bigWinTiming = " Normal 2x+ cycle active. Bet safely on blue rounds (2x+).";
        }

        // Analysis Dashboard එක සැකසීම
        String summary = "<b>--- Aviator / Crash 100 Round Analysis ---</b><br><br>" +
                " Total Rounds Analyzed: <b>" + totalRounds + "</b><br>" +
                " Pink Multipliers (10x+): <font color='#FF1493'><b>" + count10x + "</b></font><br>" +
                " Blue Multipliers (2x - 9.99x): <font color='#1E90FF'><b>" + count2x + "</b></font><br>" +
                " Low Multipliers (<2x): <b>" + lowRounds + "</b><br>" +
                " Average Multiplier: <b>" + String.format("%.2f", avgMultiplier) + "x</b><br><br>" +
                "<b>1. Platform Financial Status:</b><br>" + platformState + "<br><br>" +
                "<b>2. Bet Volume & Margin Analysis:</b><br>" + betVolumeAnalysis + "<br><br>" +
                "<b>3. Big Win & Timing Prediction:</b><br>" + bigWinTiming;

        txtAnalysisResult.setText(Html.fromHtml(summary, Html.FROM_HTML_MODE_LEGACY));
    }
}
