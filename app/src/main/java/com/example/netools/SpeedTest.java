package com.example.netools;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Activité gérant le Test de Débit (Speedtest).
 * Utilise le protocole HTTP pour télécharger un fichier témoin et mesurer la vitesse
 * de réception (Download) ainsi que la latence (Ping).
 */
public class SpeedTest extends AppCompatActivity {

    // URL d'un fichier de 10Mo hébergé sur des serveurs optimisés pour le test (OVH Proof)
    private static final String URL = "https://proof.ovh.net/files/10Mb.dat";

    private TextView tvSpeed, tvPing, tvMin, tvMax;
    private Button btnStart;

    // Indicateurs visuels pour l'expérience utilisateur (Quality of Experience)
    private ProgressBar progressBarSurfing, progressBarStreaming, progressBarGaming;

    // Seuils de référence pour les barres de progression (en Mbps)
    private static final double SURFING_MAX = 10.0;
    private static final double STREAMING_MAX = 25.0; // Recommandé pour la 4K
    private static final double GAMING_MAX = 50.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);

        // Initialisation des vues
        tvSpeed = findViewById(R.id.tvSpeed);
        tvPing = findViewById(R.id.tvPing);
        tvMin = findViewById(R.id.tvMin);
        tvMax = findViewById(R.id.tvMax);
        btnStart = findViewById(R.id.btnStart);
        progressBarSurfing = findViewById(R.id.progressBarSurfing);
        progressBarStreaming = findViewById(R.id.progressBarStreaming);
        progressBarGaming = findViewById(R.id.progressBarGaming);

        // Les barres vont de 0 à 100%
        progressBarSurfing.setMax(100);
        progressBarStreaming.setMax(100);
        progressBarGaming.setMax(100);

        // Au clic, on désactive le bouton et on lance le test dans un Thread séparé
        btnStart.setOnClickListener(v -> {
            btnStart.setEnabled(false);
            tvSpeed.setText(getString(R.string.loading));
            new Thread(this::runTest).start();
        });
    }

    /**
     * Change la couleur de la barre de progression selon le pourcentage.
     * < 33% : Rouge | 33-66% : Orange | > 66% : Vert
     */
    private void updateProgressBarColor(ProgressBar progressBar, int progress) {
        int color;
        if (progress < 33) {
            color = Color.RED;
        } else if (progress < 66) {
            color = Color.parseColor("#FFA500"); // Orange
        } else {
            color = Color.parseColor("#4CAF50"); // Vert Material Design
        }
        // runOnUiThread est vital ici car cette méthode est appelée depuis le Thread réseau
        runOnUiThread(() -> progressBar.setProgressTintList(ColorStateList.valueOf(color)));
    }

    /**
     * Logique principale du test (Exécutée en arrière-plan).
     */
    private void runTest() {
        OkHttpClient client = new OkHttpClient();

        // --- ÉTAPE 1 : PING (Latence) ---
        long pingStart = System.currentTimeMillis();
        // Requête HEAD (ne télécharge pas le corps, juste les en-têtes) pour être rapide
        Request pingRequest = new Request.Builder().url(URL).head().build();
        try (Response ignored = client.newCall(pingRequest).execute()) {
            long ping = System.currentTimeMillis() - pingStart;
            runOnUiThread(() -> tvPing.setText(String.format(Locale.getDefault(), "%d ms", ping)));
        } catch (IOException ignored) {}

        // --- ÉTAPE 2 : DOWNLOAD (Débit) ---
        Request request = new Request.Builder().url(URL).build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) throw new IOException(getString(R.string.error_network));

            InputStream is = body.byteStream();
            byte[] buf = new byte[8192]; // Tampon de 8 Ko
            long total = 0; // Total d'octets lus
            long start = System.currentTimeMillis();

            // Variables pour les statistiques
            double min = Double.MAX_VALUE;
            double max = 0;
            int read;

            // Boucle de lecture du flux
            while ((read = is.read(buf)) != -1) {
                total += read;
                long elapsed = System.currentTimeMillis() - start;

                // Mise à jour de l'UI toutes les 500ms environ pour éviter de surcharger le Thread UI
                if (elapsed > 500) {
                    // Calcul : (Octets * 8 bits) / 1 million = Mégabits
                    // Temps en secondes = ms / 1000
                    double currentSpeed = (total * 8.0 / 1000000.0) / (elapsed / 1000.0);

                    if (currentSpeed < min) min = currentSpeed;
                    if (currentSpeed > max) max = currentSpeed;

                    // Variables finales nécessaires pour l'expression lambda ci-dessous
                    final double fSpeed = currentSpeed;
                    final double fMin = min;
                    final double fMax = max;

                    // Calcul des pourcentages pour les usages (Navigation, Streaming, Jeu)
                    int pSurfing = (int) Math.min(100, (fSpeed / SURFING_MAX) * 100);
                    int pStreaming = (int) Math.min(100, (fSpeed / STREAMING_MAX) * 100);
                    int pGaming = (int) Math.min(100, (fSpeed / GAMING_MAX) * 100);

                    // Mise à jour visuelle (Couleurs + Textes)
                    updateProgressBarColor(progressBarSurfing, pSurfing);
                    updateProgressBarColor(progressBarStreaming, pStreaming);
                    updateProgressBarColor(progressBarGaming, pGaming);

                    runOnUiThread(() -> {
                        tvSpeed.setText(String.format(Locale.getDefault(), "%.1f Mbps", fSpeed));
                        tvMin.setText(String.format(Locale.getDefault(), "%.1f Mbps", fMin));
                        tvMax.setText(String.format(Locale.getDefault(), "%.1f Mbps", fMax));
                        progressBarSurfing.setProgress(pSurfing);
                        progressBarStreaming.setProgress(pStreaming);
                        progressBarGaming.setProgress(pGaming);
                    });
                }
            }
        } catch (IOException e) {
            runOnUiThread(() -> tvSpeed.setText(getString(R.string.error)));
        } finally {
            // Réactivation du bouton une fois fini
            runOnUiThread(() -> btnStart.setEnabled(true));
        }
    }
}