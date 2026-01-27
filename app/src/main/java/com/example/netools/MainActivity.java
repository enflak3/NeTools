package com.example.netools;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * ACTIVITÉ PRINCIPALE (MENU D'ACCUEIL)
 * C'est le point d'entrée de l'application. Elle contient le menu
 * permettant d'accéder aux différents outils réseau.
 * 
 * Travail réalisé par : [Votre Nom]
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Chargement des préférences de langue au lancement
        LanguageManager.loadLocale(this);
        super.onCreate(savedInstanceState);
        
        // Activation du mode bord-à-bord (design moderne)
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Gestion automatique des marges pour les barres système (statut, navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /* --- CONFIGURATION DES BOUTONS DU MENU --- */

        // Bouton vers le Speed Test
        findViewById(R.id.button).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SpeedTest.class));
        });

        // Bouton vers le Calculateur d'IP
        findViewById(R.id.button3).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, IpCalculatorActivity.class));
        });


        // Bouton vers le Trace Route
        findViewById(R.id.btnTraceRoute).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TraceRouteActivity.class));
        });

        // Bouton vers SSH / Telnet
        findViewById(R.id.button4).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SshTelnetActivity.class));
        });

        // Bouton Paramètres (Icône en haut à droite)
        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }
}
