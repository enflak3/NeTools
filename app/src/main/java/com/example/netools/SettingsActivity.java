package com.example.netools;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private boolean isInitialDisplay = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Appliquer la langue avant tout
        LanguageManager.loadLocale(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Spinner spinnerLanguage = findViewById(R.id.spinnerLanguage);

        // Language Spinner
        String[] languages = {getString(R.string.lang_fr), getString(R.string.lang_en)};
        String[] codes = {"fr", "en"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        // Sélectionner la langue actuelle
        String currentLang = LanguageManager.getLangCode(this);
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLang)) {
                spinnerLanguage.setSelection(i);
                break;
            }
        }

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialDisplay) {
                    isInitialDisplay = false;
                    return;
                }
                String selectedCode = codes[position];
                if (!selectedCode.equals(LanguageManager.getLangCode(SettingsActivity.this))) {
                    LanguageManager.setLocale(SettingsActivity.this, selectedCode);
                    
                    // Redémarrer l'application pour appliquer partout proprement
                    Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
}
