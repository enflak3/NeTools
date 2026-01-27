package com.example.netools;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// Bibliothèques tierces pour les protocoles réseaux
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.commons.net.telnet.TelnetClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Activité gérant le terminal distant SSH et Telnet.
 * Elle permet d'ouvrir un shell interactif sur une machine distante.
 */
public class SshTelnetActivity extends AppCompatActivity {

    // Éléments d'interface
    private EditText etHost, etPort, etUser, etPassword, etCommand;
    private TextView tvTerminal; // L'écran noir du terminal
    private ScrollView svTerminal; // Pour faire défiler le texte
    private Button btnConnect, btnSend;
    private RadioButton rbSsh; // Choix du protocole

    // Objets de connexion
    private Session sshSession;
    private ChannelShell sshChannel; // Canal spécifique pour un shell interactif
    private TelnetClient telnetClient;

    // Flux d'entrée/sortie génériques (polymorphisme : marche pour SSH et Telnet)
    private InputStream inputStream;  // Pour lire ce que le serveur répond
    private OutputStream outputStream; // Pour envoyer des commandes au serveur

    private boolean isConnected = false;
    private static final String PREFS_NAME = "NeToolsPrefs";
    private static final String KEY_LAST_IP = "last_ip";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.loadLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ssh_telnet);

        // Initialisation des vues
        etHost = findViewById(R.id.etHost);
        etPort = findViewById(R.id.etPort);
        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        etCommand = findViewById(R.id.etCommand);
        tvTerminal = findViewById(R.id.tvTerminal);
        svTerminal = findViewById(R.id.svTerminal);
        btnConnect = findViewById(R.id.btnConnect);
        btnSend = findViewById(R.id.btnSend);
        rbSsh = findViewById(R.id.rbSsh);

        // --- PERSISTANCE ---
        // Restauration de la dernière IP utilisée
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastIp = prefs.getString(KEY_LAST_IP, "");
        etHost.setText(lastIp);

        // Sauvegarde automatique à la frappe
        etHost.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                prefs.edit().putString(KEY_LAST_IP, s.toString()).apply();
            }
        });

        // Gestion du bouton Connexion/Déconnexion
        btnConnect.setOnClickListener(v -> {
            if (!isConnected) {
                connect();
            } else {
                disconnect();
            }
        });

        // Gestion de l'envoi de commandes
        btnSend.setOnClickListener(v -> {
            String command = etCommand.getText().toString();
            if (isConnected && !command.isEmpty()) {
                // On ajoute \n pour simuler la touche "Entrée"
                sendCommand(command + "\n");
                etCommand.setText("");
            }
        });

        // Changement automatique du port par défaut (22 pour SSH, 23 pour Telnet)
        rbSsh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etPort.setText("22");
            } else {
                etPort.setText("23");
            }
        });
    }

    /**
     * Établit la connexion (SSH ou Telnet) dans un Thread séparé.
     */
    private void connect() {
        String host = etHost.getText().toString();
        int port = Integer.parseInt(etPort.getText().toString());
        String user = etUser.getText().toString();
        String password = etPassword.getText().toString();
        boolean isSsh = rbSsh.isChecked();

        new Thread(() -> {
            try {
                if (isSsh) {
                    // --- CONNEXION SSH (via JSch) ---
                    JSch jsch = new JSch();
                    sshSession = jsch.getSession(user, host, port);
                    sshSession.setPassword(password);

                    // Configuration pour ignorer la vérification stricte de la clé hôte (évite les erreurs "Unknown Host")
                    // Note : En prod, c'est une faille de sécurité, mais acceptable pour un projet étudiant.
                    Properties config = new Properties();
                    config.put("StrictHostKeyChecking", "no");
                    sshSession.setConfig(config);
                    sshSession.connect();

                    // Ouverture d'un canal "shell" (interactif)
                    sshChannel = (ChannelShell) sshSession.openChannel("shell");
                    inputStream = sshChannel.getInputStream();
                    outputStream = sshChannel.getOutputStream();
                    sshChannel.connect();
                } else {
                    // --- CONNEXION TELNET (via Apache Commons Net) ---
                    telnetClient = new TelnetClient();
                    telnetClient.connect(host, port);
                    inputStream = telnetClient.getInputStream();
                    outputStream = telnetClient.getOutputStream();
                }

                isConnected = true;

                // Mise à jour de l'UI (Succès)
                runOnUiThread(() -> {
                    btnConnect.setText(R.string.disconnect);
                    appendToTerminal(getString(R.string.connected_to, host));
                });

                // Lancement de la boucle d'écoute pour lire les réponses du serveur
                startReading();

            } catch (Exception e) {
                // Gestion des erreurs (Mauvais mot de passe, Timeout...)
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.conn_failed, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * Boucle infinie qui lit les données envoyées par le serveur
     * et les affiche dans le TextView.
     */
    private void startReading() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int i;
            try {
                // read() est bloquant : il attend que des données arrivent
                while (isConnected && (i = inputStream.read(buffer)) != -1) {
                    // Conversion des octets reçus en String
                    String output = new String(buffer, 0, i);

                    // Affichage dans le Thread UI
                    runOnUiThread(() -> appendToTerminal(output));
                }
            } catch (Exception e) {
                if (isConnected) {
                    runOnUiThread(() -> appendToTerminal("\n" + getString(R.string.disconnect) + ": " + e.getMessage() + "\n"));
                    disconnect();
                }
            }
        }).start();
    }

    /**
     * Envoie une commande au serveur via l'OutputStream.
     */
    private void sendCommand(String command) {
        new Thread(() -> {
            try {
                outputStream.write(command.getBytes());
                outputStream.flush(); // Force l'envoi immédiat
            } catch (Exception e) {
                runOnUiThread(() -> appendToTerminal("\n" + getString(R.string.error) + " : " + e.getMessage() + "\n"));
            }
        }).start();
    }

    /**
     * Ferme proprement les connexions et flux.
     */
    private void disconnect() {
        isConnected = false;
        new Thread(() -> {
            try {
                if (sshChannel != null) sshChannel.disconnect();
                if (sshSession != null) sshSession.disconnect();
                if (telnetClient != null) telnetClient.disconnect();
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (Exception ignored) {}

            runOnUiThread(() -> {
                btnConnect.setText(R.string.connect);
                appendToTerminal("\n" + getString(R.string.disconnect) + ".\n");
            });
        }).start();
    }

    /**
     * Ajoute du texte au terminal et fait défiler vers le bas.
     */
    private void appendToTerminal(String text) {
        tvTerminal.append(text);
        // post() met cette action dans la file d'attente de l'UI pour qu'elle s'exécute APRES la mise à jour du texte
        svTerminal.post(() -> svTerminal.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect(); // Sécurité : on coupe tout si l'appli est fermée
    }
}