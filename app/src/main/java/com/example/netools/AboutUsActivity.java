package com.example.netools;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class AboutUsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.loadLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);
    }
}
