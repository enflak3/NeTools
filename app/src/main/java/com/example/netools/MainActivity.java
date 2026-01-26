package com.example.netools;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.loadLocale(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.button).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SpeedTest.class));
        });

        findViewById(R.id.button3).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, IpCalculatorActivity.class));
        });

        findViewById(R.id.button4).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SshTelnetActivity.class));
        });

        findViewById(R.id.btnTraceRoute).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TraceRouteActivity.class));
        });

        ImageButton btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }
}
