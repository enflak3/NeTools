package com.example.netools;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class SshTelnetActivity extends AppCompatActivity {

    private EditText etHost, etPort, etUser, etPassword, etCommand;
    private TextView tvTerminal;
    private Button btnConnect, btnSend;
    private RadioButton rbSsh;

    private Session sshSession;
    private ChannelShell sshChannel;
    private TelnetClient telnetClient;
    private InputStream inputStream;
    private OutputStream outputStream;

    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ssh_telnet);

        etHost = findViewById(R.id.etHost);
        etPort = findViewById(R.id.etPort);
        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        etCommand = findViewById(R.id.etCommand);
        tvTerminal = findViewById(R.id.tvTerminal);
        btnConnect = findViewById(R.id.btnConnect);
        btnSend = findViewById(R.id.btnSend);
        rbSsh = findViewById(R.id.rbSsh);

        btnConnect.setOnClickListener(v -> {
            if (!isConnected) {
                connect();
            } else {
                disconnect();
            }
        });

        btnSend.setOnClickListener(v -> {
            String command = etCommand.getText().toString();
            if (isConnected && !command.isEmpty()) {
                sendCommand(command + "\n");
                etCommand.setText("");
            }
        });

        rbSsh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etPort.setText("22");
            } else {
                etPort.setText("23");
            }
        });
    }

    private void connect() {
        String host = etHost.getText().toString();
        int port = Integer.parseInt(etPort.getText().toString());
        String user = etUser.getText().toString();
        String password = etPassword.getText().toString();
        boolean isSsh = rbSsh.isChecked();

        new Thread(() -> {
            try {
                if (isSsh) {
                    JSch jsch = new JSch();
                    sshSession = jsch.getSession(user, host, port);
                    sshSession.setPassword(password);
                    Properties config = new Properties();
                    config.put("StrictHostKeyChecking", "no");
                    sshSession.setConfig(config);
                    sshSession.connect();

                    sshChannel = (ChannelShell) sshSession.openChannel("shell");
                    inputStream = sshChannel.getInputStream();
                    outputStream = sshChannel.getOutputStream();
                    sshChannel.connect();
                } else {
                    telnetClient = new TelnetClient();
                    telnetClient.connect(host, port);
                    inputStream = telnetClient.getInputStream();
                    outputStream = telnetClient.getOutputStream();
                }

                isConnected = true;
                runOnUiThread(() -> {
                    btnConnect.setText(R.string.disconnect);
                    appendToTerminal(getString(R.string.connected_to, host));
                });

                startReading();

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.conn_failed, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void startReading() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int i;
            try {
                while (isConnected && (i = inputStream.read(buffer)) != -1) {
                    String output = new String(buffer, 0, i);
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

    private void sendCommand(String command) {
        new Thread(() -> {
            try {
                outputStream.write(command.getBytes());
                outputStream.flush();
            } catch (Exception e) {
                runOnUiThread(() -> appendToTerminal("\n" + getString(R.string.error) + " : " + e.getMessage() + "\n"));
            }
        }).start();
    }

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

    private void appendToTerminal(String text) {
        tvTerminal.append(text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }
}
