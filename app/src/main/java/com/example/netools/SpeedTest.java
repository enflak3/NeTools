package com.example.netools;

import android.content.res.ColorStateList; // Ajouté
import android.graphics.Color; // Ajouté
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

public class SpeedTest extends AppCompatActivity {

    private static final String URL = "https://proof.ovh.net/files/10Mb.dat";
    private TextView tvSpeed, tvPing, tvMin, tvMax;
    private Button btnStart;
    private ProgressBar progressBarSurfing, progressBarStreaming, progressBarGaming;

    // Seuils de vitesse en Mbps
    private static final double SURFING_MAX = 10.0;
    private static final double STREAMING_MAX = 25.0;
    private static final double GAMING_MAX = 50.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);

        tvSpeed = findViewById(R.id.tvSpeed);
        tvPing = findViewById(R.id.tvPing);
        tvMin = findViewById(R.id.tvMin);
        tvMax = findViewById(R.id.tvMax);
        btnStart = findViewById(R.id.btnStart);
        progressBarSurfing = findViewById(R.id.progressBarSurfing);
        progressBarStreaming = findViewById(R.id.progressBarStreaming);
        progressBarGaming = findViewById(R.id.progressBarGaming);

        // Initialisation des progress bars à 100 max
        progressBarSurfing.setMax(100);
        progressBarStreaming.setMax(100);
        progressBarGaming.setMax(100);

        btnStart.setOnClickListener(v -> {
            btnStart.setEnabled(false);
            tvSpeed.setText("En cours...");

            // Réinitialisation avec la méthode couleur (remet à 0 et probablement rouge)
            updateProgressWithColor(progressBarSurfing, 0);
            updateProgressWithColor(progressBarStreaming, 0);
            updateProgressWithColor(progressBarGaming, 0);

            new Thread(this::runTest).start();
        });
    }

    private void runTest() {
        OkHttpClient client = new OkHttpClient();

        // 1. Mesure de la latence (Ping simplifié)
        long pingStart = System.currentTimeMillis();
        Request pingRequest = new Request.Builder().url(URL).head().build();
        try (Response ignored = client.newCall(pingRequest).execute()) {
            long ping = System.currentTimeMillis() - pingStart;
            runOnUiThread(() -> tvPing.setText(String.format(Locale.getDefault(), "%d ms", ping)));
        } catch (IOException ignored) {}

        // 2. Test de débit
        Request request = new Request.Builder().url(URL).build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) throw new IOException("Erreur réseau");

            InputStream is = body.byteStream();
            byte[] buf = new byte[8192];
            long total = 0;
            long start = System.currentTimeMillis();
            double min = Double.MAX_VALUE;
            double max = 0;
            int read;

            while ((read = is.read(buf)) != -1) {
                total += read;
                long elapsed = System.currentTimeMillis() - start;

                if (elapsed > 500) { // On commence à calculer après 500ms
                    double currentSpeed = (total * 8.0 / 1000000.0) / (elapsed / 1000.0);

                    if (currentSpeed < min) min = currentSpeed;
                    if (currentSpeed > max) max = currentSpeed;

                    final double fSpeed = currentSpeed;
                    final double fMin = min;
                    final double fMax = max;

                    // Calcul des pourcentages pour les barres
                    int progressSurfing = (int) Math.min(100, (fSpeed / SURFING_MAX) * 100);
                    int progressStreaming = (int) Math.min(100, (fSpeed / STREAMING_MAX) * 100);
                    int progressGaming = (int) Math.min(100, (fSpeed / GAMING_MAX) * 100);

                    runOnUiThread(() -> {
                        tvSpeed.setText(String.format(Locale.getDefault(), "%.1f Mbps", fSpeed));
                        tvMin.setText(String.format(Locale.getDefault(), "%.1f Mbps", fMin));
                        tvMax.setText(String.format(Locale.getDefault(), "%.1f Mbps", fMax));

                        // Utilisation de la nouvelle méthode pour mettre à jour AVEC couleur
                        updateProgressWithColor(progressBarSurfing, progressSurfing);
                        updateProgressWithColor(progressBarStreaming, progressStreaming);
                        updateProgressWithColor(progressBarGaming, progressGaming);
                    });
                }
            }
        } catch (IOException e) {
            runOnUiThread(() -> tvSpeed.setText("Erreur"));
        } finally {
            runOnUiThread(() -> btnStart.setEnabled(true));
        }
    }

    /**
     * Met à jour la ProgressBar et change sa couleur selon le niveau de remplissage.
     * < 1/3 : Rouge
     * 1/3 à 2/3 : Orange
     * > 2/3 : Vert
     */
    private void updateProgressWithColor(ProgressBar progressBar, int progress) {
        progressBar.setProgress(progress);

        int max = progressBar.getMax();
        double ratio = (double) progress / max;
        int color;

        if (ratio < (1.0 / 3.0)) {
            // Moins de 1/3 -> Rouge
            color = Color.RED;
        } else if (ratio < (2.0 / 3.0)) {
            // Entre 1/3 et 2/3 -> Orange
            color = Color.parseColor("#FFA500");
        } else {
            // Plus de 2/3 -> Vert
            color = Color.parseColor("#4CAF50"); // Ou Color.parseColor("#4CAF50") pour un vert plus doux
        }

        progressBar.setProgressTintList(ColorStateList.valueOf(color));
    }
}