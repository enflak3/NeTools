package com.example.netools;

import android.os.Bundle;
import android.widget.Button;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);

        tvSpeed = findViewById(R.id.tvSpeed);
        tvPing = findViewById(R.id.tvPing);
        tvMin = findViewById(R.id.tvMin);
        tvMax = findViewById(R.id.tvMax);
        btnStart = findViewById(R.id.btnStart);

        btnStart.setOnClickListener(v -> {
            btnStart.setEnabled(false);
            tvSpeed.setText("En cours...");
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

                if (elapsed > 500) { // On commence à calculer après 500ms pour plus de stabilité
                    double currentSpeed = (total * 8.0 / 1000000.0) / (elapsed / 1000.0);

                    if (currentSpeed < min) min = currentSpeed;
                    if (currentSpeed > max) max = currentSpeed;

                    final double fSpeed = currentSpeed;
                    final double fMin = min;
                    final double fMax = max;
                    runOnUiThread(() -> {
                        tvSpeed.setText(String.format(Locale.getDefault(), "%.1f Mbps", fSpeed));
                        tvMin.setText(String.format(Locale.getDefault(), "%.1f Mbps", fMin));
                        tvMax.setText(String.format(Locale.getDefault(), "%.1f Mbps", fMax));
                    });
                }
            }
        } catch (IOException e) {
            runOnUiThread(() -> tvSpeed.setText("Erreur"));
        } finally {
            runOnUiThread(() -> btnStart.setEnabled(true));
        }
    }
}