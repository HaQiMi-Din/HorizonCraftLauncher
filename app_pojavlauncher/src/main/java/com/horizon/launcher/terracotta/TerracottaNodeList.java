/*
 * Horizon Craft Launcher - Terracotta (陶瓦联机) public node list.
 *
 * Adapted from Fold Craft Launcher / HMCL Terracotta integration (GPL-3.0).
 * The node list service is provided by the Terracotta project.
 */
package com.horizon.launcher.terracotta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetches the list of public EasyTier nodes that Terracotta uses, so the
 * launcher can seed the P2P network with extra nodes.
 */
public final class TerracottaNodeList {

    private static final String NODE_LIST_URL = "https://terracotta.glavo.site/nodes";

    private static final AtomicReference<List<URI>> CACHE = new AtomicReference<>(null);

    private TerracottaNodeList() {}

    /** Returns the cached node list, or fetches it once. Never returns null. */
    public static List<URI> fetch() {
        List<URI> cached = CACHE.get();
        if (cached != null) return cached;
        synchronized (TerracottaNodeList.class) {
            cached = CACHE.get();
            if (cached != null) return cached;
            List<URI> list = doFetch();
            CACHE.set(list);
            return list;
        }
    }

    private static List<URI> doFetch() {
        List<URI> result = new ArrayList<>();
        try {
            URL url = new URL(NODE_LIST_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "HorizonCraftLauncher/1.0 (Terracotta)");
            conn.setRequestProperty("Accept", "application/json");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JsonElement root = JsonParser.parseString(sb.toString());
                if (root.isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray();
                    for (JsonElement el : arr) {
                        if (!el.isJsonObject()) continue;
                        JsonObject node = el.getAsJsonObject();
                        if (!node.has("url")) continue;
                        try {
                            result.add(new URI(node.get("url").getAsString()));
                        } catch (Exception ignored) {}
                    }
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            Logger.getLogger("TerracottaNodeList").log(Level.WARNING,
                    "Failed to fetch terracotta node list", e);
        }
        return result;
    }
}
