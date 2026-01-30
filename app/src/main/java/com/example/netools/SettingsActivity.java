package com.example.netools;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Appliquer la langue avant tout
        LanguageManager.loadLocale(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        AutoCompleteTextView spinnerLanguage = findViewById(R.id.spinnerLanguage);

        // Données pour le menu déroulant
        String[] languages = {getString(R.string.lang_fr), getString(R.string.lang_en)};
        String[] codes = {"fr", "en"};

        // Adaptateur pour l'AutoCompleteTextView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, languages);
        spinnerLanguage.setAdapter(adapter);

        // Pré-remplir avec la langue actuelle
        String currentLangCode = LanguageManager.getLangCode(this);
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLangCode)) {
                spinnerLanguage.setText(languages[i], false); // false pour ne pas filtrer
                break;
            }
        }

        // Gérer le changement de langue
        spinnerLanguage.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCode = codes[position];
            if (!selectedCode.equals(LanguageManager.getLangCode(SettingsActivity.this))) {
                LanguageManager.setLocale(SettingsActivity.this, selectedCode);

                // Redémarrer pour appliquer la langue partout
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Bouton À propos de nous
        findViewById(R.id.btnAboutUs).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, AboutUsActivity.class));
        });

        // Easter Egg (Rickroll)
        findViewById(R.id.btnEasterEgg).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/dQw4w9WgXcQ?si=UE5DLZeGWSQpd2Cq"));
            startActivity(intent);
        });
    }
}
