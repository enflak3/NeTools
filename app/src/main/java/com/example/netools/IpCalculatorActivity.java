package com.example.netools;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class IpCalculatorActivity extends AppCompatActivity {

    private EditText ipAddressInput;
    private EditText cidrInput;
    private Button calculateButton;
    private TextView networkAddressText;
    private TextView broadcastAddressText;
    private TextView hostRangeText;
    private TextView subnetMaskText;
    private TextView wildcardMaskText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip_calculator);

        ipAddressInput = findViewById(R.id.ipAddressInput);
        cidrInput = findViewById(R.id.cidrInput);
        calculateButton = findViewById(R.id.calculateButton);
        networkAddressText = findViewById(R.id.networkAddressText);
        broadcastAddressText = findViewById(R.id.broadcastAddressText);
        hostRangeText = findViewById(R.id.hostRangeText);
        subnetMaskText = findViewById(R.id.subnetMaskText);
        wildcardMaskText = findViewById(R.id.wildcardMaskText);

        calculateButton.setOnClickListener(v -> calculate());
    }

    private void calculate() {
        String ipAddressStr = ipAddressInput.getText().toString();
        String cidrStr = cidrInput.getText().toString();

        if (ipAddressStr.isEmpty() || cidrStr.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer une adresse IP et un CIDR", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long ip = ipToLong(ipAddressStr);
            int cidr = Integer.parseInt(cidrStr);

            if (cidr < 0 || cidr > 32) {
                Toast.makeText(this, "CIDR invalide (doit être entre 0 et 32)", Toast.LENGTH_SHORT).show();
                return;
            }

            long mask = (cidr == 0) ? 0L : (-1L << (32 - cidr));
            long network = ip & mask;
            long wildcard = ~mask;
            long broadcast = network | wildcard;

            long firstHost = network + 1;
            long lastHost = broadcast - 1;

            networkAddressText.setText("Adresse réseau: " + longToIp(network));
            broadcastAddressText.setText("Adresse broadcast: " + longToIp(broadcast));

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
            result |= (long)part << (24 - (8 * i));
        }
        return result;
    }

    private String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." +
               ((ip >> 16) & 0xFF) + "." +
               ((ip >> 8) & 0xFF) + "." +
               (ip & 0xFF);
    }
}
