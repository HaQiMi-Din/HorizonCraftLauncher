/*
 * Horizon Craft Launcher - Terracotta (陶瓦联机) state model.
 *
 * Adapted from Fold Craft Launcher / HMCL Terracotta integration.
 * Fold Craft Launcher: Copyright (C) 2025 huangyuhui <huanghongxun2008@126.com>
 * Licensed under GPL-3.0. Terracotta itself is AGPL-3.0 by Burning_TNT.
 */
package com.horizon.launcher.terracotta;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The state machine reported by the Terracotta native backend as JSON:
 * <pre>
 *   {"index":N,"state":"waiting"}
 *   {"index":N,"state":"host-scanning"}
 *   {"index":N,"state":"host-starting"}
 *   {"index":N,"state":"host-ok","room":"u/xxxx-xxxx-xxxx-xxxx","profile_index":0,"profiles":[...]}
 *   {"index":N,"state":"guest-connecting"}
 *   {"index":N,"state":"guest-starting","difficulty":2}
 *   {"index":N,"state":"guest-ok","url":"127.0.0.1:25565","profile_index":0,"profiles":[...]}
 *   {"index":N,"state":"exception","type":0}
 * </pre>
 * The {@code index} field is monotonically increasing; a larger index means a newer state.
 */
public abstract class TerracottaState {

    public final int index;

    protected TerracottaState(int index) {
        this.index = index;
    }

    @NonNull
    @Override
    public String toString() {
        String simple = getClass().getSimpleName();
        return simple.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    /* ---------------- Concrete states ---------------- */

    /** Idle; nothing is happening. */
    public static final class Waiting extends TerracottaState {
        Waiting(int index) { super(index); }
    }

    /** Host: looking for an available public node. */
    public static final class HostScanning extends TerracottaState {
        HostScanning(int index) { super(index); }
    }

    /** Host: virtual network is starting. */
    public static final class HostStarting extends TerracottaState {
        HostStarting(int index) { super(index); }
    }

    /** Host: room created, invite code is available. */
    public static final class HostOK extends TerracottaState {
        private final String room;
        private final int profileIndex;
        private final List<TerracottaProfile> profiles;

        HostOK(int index, String room, int profileIndex, List<TerracottaProfile> profiles) {
            super(index);
            this.room = room;
            this.profileIndex = profileIndex;
            this.profiles = profiles;
        }

        public String getRoomCode() { return room; }

        public int getProfileIndex() { return profileIndex; }

        public List<TerracottaProfile> getProfiles() { return profiles; }
    }

    /** Guest: connecting to the requested room. */
    public static final class GuestConnecting extends TerracottaState {
        GuestConnecting(int index) { super(index); }
    }

    /** Guest: P2P tunnel is being established. */
    public static final class GuestStarting extends TerracottaState {
        public enum Difficulty { UNKNOWN, EASIEST, SIMPLE, MEDIUM, TOUGH }
        private final Difficulty difficulty;
        GuestStarting(int index, Difficulty difficulty) {
            super(index);
            this.difficulty = difficulty;
        }
        public Difficulty getDifficulty() { return difficulty; }
    }

    /** Guest: connected, a local address is available to join in Minecraft. */
    public static final class GuestOK extends TerracottaState {
        private final String url;
        private final int profileIndex;
        private final List<TerracottaProfile> profiles;

        GuestOK(int index, String url, int profileIndex, List<TerracottaProfile> profiles) {
            super(index);
            this.url = url;
            this.profileIndex = profileIndex;
            this.profiles = profiles;
        }

        /** e.g. "127.0.0.1:25565" */
        public String getUrl() { return url; }

        public int getProfileIndex() { return profileIndex; }

        public List<TerracottaProfile> getProfiles() { return profiles; }
    }

    /** An error occurred. */
    public static final class Exception extends TerracottaState {
        public enum Type {
            PING_HOST_FAIL, PING_HOST_RST, GUEST_ET_CRASH,
            HOST_ET_CRASH, PING_SERVER_RST, SCAFFOLDING_INVALID_RESPONSE
        }
        private static final Type[] LOOKUP = Type.values();
        private final Type type;
        Exception(int index, int type) {
            super(index);
            this.type = (type >= 0 && type < LOOKUP.length) ? LOOKUP[type] : Type.PING_HOST_FAIL;
        }
        public Type getType() { return type; }
    }

    /* ---------------- JSON parsing ---------------- */

    /**
     * Parse a state JSON string returned by the native backend.
     * @return the parsed state, or {@code null} if the JSON is malformed / unknown.
     */
    @Nullable
    public static TerracottaState parse(String json) {
        JsonObject obj;
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) return null;
            obj = el.getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
        int index = obj.has("index") ? obj.get("index").getAsInt() : -1;
        String state = obj.has("state") ? obj.get("state").getAsString() : "";
        switch (state) {
            case "waiting":
                return new Waiting(index);
            case "host-scanning":
                return new HostScanning(index);
            case "host-starting":
                return new HostStarting(index);
            case "host-ok": {
                String room = obj.has("room") ? obj.get("room").getAsString() : null;
                int pidx = obj.has("profile_index") ? obj.get("profile_index").getAsInt() : 0;
                return new HostOK(index, room, pidx, parseProfiles(obj));
            }
            case "guest-connecting":
                return new GuestConnecting(index);
            case "guest-starting": {
                GuestStarting.Difficulty d = GuestStarting.Difficulty.UNKNOWN;
                if (obj.has("difficulty")) {
                    try {
                        GuestStarting.Difficulty[] ds = GuestStarting.Difficulty.values();
                        int di = obj.get("difficulty").getAsInt();
                        if (di >= 0 && di < ds.length) d = ds[di];
                    } catch (Exception ignored) {}
                }
                return new GuestStarting(index, d);
            }
            case "guest-ok": {
                String url = obj.has("url") ? obj.get("url").getAsString() : null;
                int pidx = obj.has("profile_index") ? obj.get("profile_index").getAsInt() : 0;
                return new GuestOK(index, url, pidx, parseProfiles(obj));
            }
            case "exception": {
                int t = obj.has("type") ? obj.get("type").getAsInt() : 0;
                return new Exception(index, t);
            }
            default:
                return null;
        }
    }

    private static List<TerracottaProfile> parseProfiles(JsonObject obj) {
        List<TerracottaProfile> list = new ArrayList<>();
        if (obj.has("profiles") && obj.get("profiles").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("profiles")) {
                try {
                    list.add(TerracottaProfile.fromJson(e.getAsJsonObject()));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}
