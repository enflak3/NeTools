package com.example.netools;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bouton SpeedTest (button)
        findViewById(R.id.button).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SpeedTest.class));
        });

        // Bouton Calculateur IP (button3)
        findViewById(R.id.button3).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, IpCalculatorActivity.class));
        });

        // Bouton SSH - Telnet (button4)
        findViewById(R.id.button4).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SshTelnetActivity.class));
        });
    }
}
