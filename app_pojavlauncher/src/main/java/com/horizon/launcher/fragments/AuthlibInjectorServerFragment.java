package com.horizon.launcher.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.horizon.launcher.PojavProfile;
import com.horizon.launcher.R;
import com.horizon.launcher.Tools;
import com.horizon.launcher.value.MinecraftAccount;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * LittleSkin / authlib-injector external (yggdrasil) login.
 * The user provides the auth server URL (default LittleSkin), username and
 * password; we authenticate against {server}/authserver/authenticate, save the
 * account and fetch the authlib-injector agent jar for game launching.
 */
public class AuthlibInjectorServerFragment extends Fragment {

    public static final String TAG = "AuthlibInjectorServerFragment";
    private static final String DEFAULT_SERVER = "https://littleskin.cn/api/yggdrasil";

    private EditText mServer, mUsername, mPassword;
    private View mLoginButton;
    private TextView mStatus;

    public AuthlibInjectorServerFragment() {
        super(R.layout.fragment_authlib_injector);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mServer = view.findViewById(R.id.authlib_edit_server);
        mUsername = view.findViewById(R.id.authlib_edit_username);
        mPassword = view.findViewById(R.id.authlib_edit_password);
        mStatus = view.findViewById(R.id.authlib_status);
        mLoginButton = view.findViewById(R.id.authlib_login_button);

        if (mServer.getText().toString().trim().isEmpty()) {
            mServer.setText(DEFAULT_SERVER);
        }
        mLoginButton.setOnClickListener(v -> login());
    }

    private void login() {
        final String server = normalizeServer(mServer.getText().toString().trim());
        final String username = mUsername.getText().toString().trim();
        final String password = mPassword.getText().toString();

        if (server.isEmpty() || username.isEmpty() || password.isEmpty()) {
            mStatus.setText(R.string.ui_authlib_err_fields);
            return;
        }

        mLoginButton.setEnabled(false);
        mStatus.setText(R.string.ui_authlib_working);
        new Thread(() -> {
            try {
                final MinecraftAccount acc = authenticate(server, username, password);
                acc.save();
                boolean jarOk = Tools.ensureAuthlibInjector(requireContext());
                PojavProfile.setCurrentProfile(requireContext(), acc.username);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            getString(R.string.ui_authlib_ok, acc.username), Toast.LENGTH_LONG).show();
                    if (!jarOk) {
                        Toast.makeText(requireContext(), R.string.ui_authlib_jar_warn, Toast.LENGTH_LONG).show();
                    }
                    Tools.swapFragment(requireActivity(), AccountFragment.class,
                            AccountFragment.TAG, null);
                });
            } catch (final Exception e) {
                requireActivity().runOnUiThread(() -> {
                    mLoginButton.setEnabled(true);
                    mStatus.setText(getString(R.string.ui_authlib_err_generic,
                            e.getMessage() == null ? "unknown" : e.getMessage()));
                });
            }
        }).start();
    }

    private static String normalizeServer(String url) {
        if (url.isEmpty()) return url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
        return url.replaceAll("/+$", "");
    }

    /** POST {server}/authserver/authenticate (yggdrasil protocol) and build the account. */
    private static MinecraftAccount authenticate(String server, String username, String password)
            throws Exception {
        URL url = new URL(server + "/authserver/authenticate");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setDoOutput(true);

        String clientToken = UUID.randomUUID().toString();
        JSONObject body = new JSONObject();
        JSONObject agent = new JSONObject();
        agent.put("name", "Minecraft");
        agent.put("version", 1);
        body.put("agent", agent);
        body.put("username", username);
        body.put("password", password);
        body.put("clientToken", clientToken);
        conn.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));

        int code = conn.getResponseCode();
        InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
        String resp = readAll(is);
        if (code != HttpURLConnection.HTTP_OK) {
            String message = extractError(resp);
            throw new Exception(message == null ? "HTTP " + code : message);
        }
        JSONObject json = new JSONObject(resp);
        JSONObject profile = json.optJSONObject("selectedProfile");
        if (profile == null) {
            throw new Exception("no selectedProfile in response");
        }
        MinecraftAccount acc = new MinecraftAccount();
        acc.isExternal = true;
        acc.authlibServer = server;
        acc.username = profile.optString("name", username);
        acc.accessToken = json.optString("accessToken", "0");
        acc.clientToken = json.optString("clientToken", clientToken);
        acc.profileId = profile.optString("id",
                "00000000-0000-0000-0000-000000000000");
        return acc;
    }

    private static String extractError(String resp) {
        try {
            JSONObject json = new JSONObject(resp);
            String m = json.optString("errorMessage", null);
            if (m != null) return m;
            String e = json.optString("error", null);
            if (e != null) return e;
        } catch (Exception ignored) {}
        return null;
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        is.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
