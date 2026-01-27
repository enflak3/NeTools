package com.example.netools;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Activité Traceroute.
 * Permet de visualiser le chemin des paquets IP vers une destination.
 * Technique utilisée : Manipulation du TTL (Time To Live) via la commande Ping.
 */
public class TraceRouteActivity extends AppCompatActivity {

    private EditText etTarget;
    private TextView tvResults;
    private ScrollView svTrace;
    private Button btnTrace;
    private boolean isTracing = false; // Flag pour arrêter la boucle si l'utilisateur annule

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.loadLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trace_route);

        etTarget = findViewById(R.id.etTarget);
        tvResults = findViewById(R.id.tvTraceResults);
        svTrace = findViewById(R.id.svTrace);
        btnTrace = findViewById(R.id.btnTrace);

        btnTrace.setOnClickListener(v -> {
            if (!isTracing) {
                // Démarrage du Trace
                String target = etTarget.getText().toString().trim();
                if (!target.isEmpty()) {
                    // Résolution DNS (Doit se faire dans un Thread car bloquant)
                    new Thread(() -> {
                        try {
                            InetAddress address = InetAddress.getByName(target);
                            // On revient sur le main thread pour lancer la logique de trace
                            runOnUiThread(() -> startTrace(address.getHostAddress()));
                        } catch (Exception e) {
                            runOnUiThread(() -> tvResults.setText("Hôte invalide : " + target));
                        }
                    }).start();
                }
            } else {
                // Arrêt manuel
                isTracing = false;
            }
        });
    }

    /**
     * Logique principale du Traceroute manuel.
     * Android ne possédant pas toujours la commande 'traceroute', on la simule
     * en envoyant des Pings avec un TTL croissant (1, 2, 3...).
     */
    private void startTrace(String targetIp) {
        isTracing = true;
        tvResults.setText(getString(R.string.tracing, targetIp) + "\n\n");
        btnTrace.setText(R.string.disconnect);

        new Thread(() -> {
            // Boucle de 1 à 30 sauts (Hops) maximum
            for (int ttl = 1; ttl <= 30 && isTracing; ttl++) {
                final int currentTtl = ttl;
                try {
                    // COMMANDE CLÉ : ping -c 1 (un seul paquet) -t <TTL> (Time To Live imposé)
                    // Si le TTL tombe à 0 avant d'arriver, le routeur renvoie "Time to live exceeded" (ICMP Type 11).
                    // C'est ce message qui nous donne l'IP du routeur intermédiaire.
                    Process process = new ProcessBuilder("ping", "-c", "1", "-t", String.valueOf(currentTtl), targetIp).start();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    String hopIp = "* * *"; // Valeur par défaut si le routeur ne répond pas (Timeout)
                    String time = "";

                    // Analyse de la réponse du Ping ligne par ligne
                    while ((line = reader.readLine()) != null) {
                        // Regex pour capturer une IPv4 (X.X.X.X)
                        Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");
                        Matcher matcher = pattern.matcher(line);

                        // Cas 1 : "From 192.168.1.1: icmp_seq=1 Time to live exceeded"
                        // C'est un routeur intermédiaire.
                        if (line.contains("From") || line.contains("from")) {
                            if (matcher.find()) {
                                hopIp = matcher.group(1);
                            }
                        }
                        // Cas 2 : "64 bytes from 8.8.8.8..."
                        // C'est la destination finale !
                        else if (line.contains("bytes from")) {
                            if (matcher.find()) {
                                hopIp = matcher.group(1);
                            }
                            if (line.contains("time=")) {
                                time = line.substring(line.indexOf("time="));
                            }
                            isTracing = false; // On arrête la boucle car on est arrivé
                        }

                        // Récupération du temps de latence si disponible
                        if (line.contains("time=") && time.isEmpty()) {
                            time = line.substring(line.indexOf("time="));
                        }
                    }

                    // Construction de la ligne de résultat (ex: "1: 192.168.1.254  time=4ms")
                    final String resultLine = String.format("%d: %s  %s\n", currentTtl, hopIp, time);

                    // Mise à jour de l'UI
                    runOnUiThread(() -> {
                        tvResults.append(resultLine);
                        // Auto-scroll vers le bas
                        svTrace.post(() -> svTrace.fullScroll(ScrollView.FOCUS_DOWN));
                    });

                    process.destroy();
                } catch (Exception e) {
                    runOnUiThread(() -> tvResults.append(currentTtl + ": Erreur\n"));
                }
            }

            // Fin du trace (ou arrêt manuel)
            runOnUiThread(() -> {
                btnTrace.setText(R.string.start_trace);
                isTracing = false;
            });
        }).start();
    }
}