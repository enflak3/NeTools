package com.example.netools;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 Activité gérant le Calculateur d'IP.
 Cette classe permet de calculer les plages d'adresses, le masque et le broadcast
 à partir d'une IP et d'un CIDR en utilisant des opérations bit à bit.
 */
public class IpCalculatorActivity extends AppCompatActivity {

    // Éléments de l'interface utilisateur (UI)
    private EditText ipAddressInput;
    private EditText cidrInput;
    private Button calculateButton;

    // Zones de texte pour l'affichage des résultats
    private TextView networkAddressText;
    private TextView broadcastAddressText;
    private TextView hostRangeText;
    private TextView subnetMaskText;
    private TextView wildcardMaskText;

    // Constantes pour la persistance des données (SharedPreferences)
    private static final String PREFS_NAME = "NeToolsPrefs";
    private static final String KEY_LAST_IP = "last_ip";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip_calculator);

        // Liaison des composants graphiques avec le code Java
        ipAddressInput = findViewById(R.id.ipAddressInput);
        cidrInput = findViewById(R.id.cidrInput);
        calculateButton = findViewById(R.id.calculateButton);
        networkAddressText = findViewById(R.id.networkAddressText);
        broadcastAddressText = findViewById(R.id.broadcastAddressText);
        hostRangeText = findViewById(R.id.hostRangeText);
        subnetMaskText = findViewById(R.id.subnetMaskText);
        wildcardMaskText = findViewById(R.id.wildcardMaskText);


        // Récupération des préférences partagées pour restaurer la dernière saisie
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastIp = prefs.getString(KEY_LAST_IP, ""); // Valeur par défaut vide
        ipAddressInput.setText(lastIp);

        // Ajout d'un écouteur (Listener) pour sauvegarder l'IP en temps réel
        // Dès que l'utilisateur tape un caractère, on sauvegarde.
        ipAddressInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 'apply()' est utilisé au lieu de 'commit()' car il est asynchrone (ne bloque pas l'UI)
                prefs.edit().putString(KEY_LAST_IP, s.toString()).apply();
            }
        });

        // Lancement du calcul au clic sur le bouton
        calculateButton.setOnClickListener(v -> calculate());
    }

    /**
     * Méthode principale de calcul.
     * Récupère les entrées, effectue les opérations binaires et met à jour l'affichage.
     */
    private void calculate() {
        String ipAddressStr = ipAddressInput.getText().toString();
        String cidrStr = cidrInput.getText().toString();

        // Validation basique : les champs ne doivent pas être vides
        if (ipAddressStr.isEmpty() || cidrStr.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer une adresse IP et un CIDR", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Conversion de l'IP String en Long pour les manips binaires
            long ip = ipToLong(ipAddressStr);
            int cidr = Integer.parseInt(cidrStr);

            // Validation du CIDR (standard IPv4)
            if (cidr < 0 || cidr > 32) {
                Toast.makeText(this, "CIDR invalide (doit être entre 0 et 32)", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- CŒUR DU CALCUL (LOGIQUE BINAIRE) ---

            // 1. Calcul du Masque : On décale des bits à 1 vers la gauche.
            // Ex: /24 -> 32-24 = 8. On décale -1 (qui est 111...111) de 8 bits vers la gauche -> 111...00000000
            long mask = (cidr == 0) ? 0L : (-1L << (32 - cidr));

            // 2. Adresse Réseau : ET Logique (&) entre l'IP et le Masque
            long network = ip & mask;

            // 3. Masque Inverse (Wildcard) : Inversement de tous les bits (~) du masque
            long wildcard = ~mask;

            // 4. Broadcast : Adresse Réseau OU (|) Wildcard (remplit la partie hôte avec des 1)
            long broadcast = network | wildcard;

            // Calcul des hôtes (Premier = Réseau + 1, Dernier = Broadcast - 1)
            long firstHost = network + 1;
            long lastHost = broadcast - 1;

            // --- MISE A JOUR DE L'AFFICHAGE ---

            networkAddressText.setText("Adresse réseau: " + longToIp(network));
            broadcastAddressText.setText("Adresse broadcast: " + longToIp(broadcast));

            // Gestion des cas particuliers (/31 et /32 n'ont pas de plage d'hôtes utilisables standard)
            if (cidr < 31) {
                hostRangeText.setText("Plage d'adresses: " + longToIp(firstHost) + " - " + longToIp(lastHost));
            } else {
                hostRangeText.setText("Plage d'adresses: N/A");
            }

            subnetMaskText.setText("Masque de sous-réseau: " + longToIp(mask));
            wildcardMaskText.setText("Masque inverse: " + longToIp(wildcard));

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Entrée numérique invalide", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Adresse IP invalide", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Convertit une adresse IP format String ("192.168.0.1") en format Long.
     * Note: On utilise 'long' (64 bits) au lieu de 'int' (32 bits) pour éviter
     * les problèmes de signes négatifs sur les adresses hautes (ex: > 128.x.x.x).
     */
    private long ipToLong(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Format d'adresse IP invalide");
        }
        long result = 0;
        for (int i = 0; i < 4; i++) {
            int part = Integer.parseInt(parts[i]);
            if (part < 0 || part > 255) {
                throw new IllegalArgumentException("Segment d'adresse IP invalide");
            }
            // Décalage de bits :
            // 1er octet décalé de 24, 2ème de 16, 3ème de 8, dernier de 0.
            // On combine le tout avec un OU Logique (|=)
            result |= (long)part << (24 - (8 * i));
        }
        return result;
    }

    /**
     * Convertit un Long en adresse IP format String.
     * Opération inverse de ipToLong.
     */
    private String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." +  // Extrait le 1er octet
                ((ip >> 16) & 0xFF) + "." +  // Extrait le 2ème octet
                ((ip >> 8) & 0xFF) + "." +   // Extrait le 3ème octet
                (ip & 0xFF);                 // Extrait le 4ème octet
    }
}