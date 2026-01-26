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

public class TraceRouteActivity extends AppCompatActivity {

    private EditText etTarget;
    private TextView tvResults;
    private ScrollView svTrace;
    private Button btnTrace;
    private boolean isTracing = false;

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
                String target = etTarget.getText().toString().trim();
                if (!target.isEmpty()) {
                    new Thread(() -> {
                        try {
                            InetAddress address = InetAddress.getByName(target);
                            runOnUiThread(() -> startTrace(address.getHostAddress()));
                        } catch (Exception e) {
                            runOnUiThread(() -> tvResults.setText("Invalid host: " + target));
                        }
                    }).start();
                }
            } else {
                isTracing = false;
            }
        });
    }

    private void startTrace(String targetIp) {
        isTracing = true;
        tvResults.setText(getString(R.string.tracing, targetIp) + "\n\n");
        btnTrace.setText(R.string.disconnect);

        new Thread(() -> {
            for (int ttl = 1; ttl <= 30 && isTracing; ttl++) {
                final int currentTtl = ttl;
                try {
                    // Utilisation de la commande ping -t (TTL) -c (count) -W (timeout)
                    // Note: Sur certains appareils Android, -t est remplacé par -T ou n'est pas supporté via exec()
                    // On tente la commande standard Linux/Android
                    Process process = new ProcessBuilder("ping", "-c", "1", "-t", String.valueOf(currentTtl), targetIp).start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    String hopIp = "* * *";
                    String time = "";

                    while ((line = reader.readLine()) != null) {
                        // Regex pour extraire l'IP (IPv4)
                        Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");
                        Matcher matcher = pattern.matcher(line);
                        
                        if (line.contains("From") || line.contains("from")) {
                            if (matcher.find()) {
                                hopIp = matcher.group(1);
                            }
                        } else if (line.contains("bytes from")) {
                            if (matcher.find()) {
                                hopIp = matcher.group(1);
                            }
                            if (line.contains("time=")) {
                                time = line.substring(line.indexOf("time="));
                            }
                            isTracing = false; // Destination atteinte
                        }
                        
                        if (line.contains("time=") && time.isEmpty()) {
                            time = line.substring(line.indexOf("time="));
                        }
                    }

                    final String resultLine = String.format("%d: %s  %s\n", currentTtl, hopIp, time);
                    runOnUiThread(() -> {
                        tvResults.append(resultLine);
                        svTrace.post(() -> svTrace.fullScroll(ScrollView.FOCUS_DOWN));
                    });

                    process.destroy();
                } catch (Exception e) {
                    runOnUiThread(() -> tvResults.append(currentTtl + ": Error\n"));
                }
            }
            runOnUiThread(() -> {
                btnTrace.setText(R.string.start_trace);
                isTracing = false;
            });
        }).start();
    }
}
