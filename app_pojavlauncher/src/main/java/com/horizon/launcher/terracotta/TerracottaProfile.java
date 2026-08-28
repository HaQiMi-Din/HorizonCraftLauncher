/*
 * Horizon Craft Launcher - Terracotta (陶瓦联机) player profile model.
 *
 * Adapted from Fold Craft Launcher / HMCL Terracotta integration (GPL-3.0).
 */
package com.horizon.launcher.terracotta;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

/** A player currently visible on the Terracotta room. */
public final class TerracottaProfile {

    public enum Kind { UNKNOWN, VANILLA, MODDED }

    private final String machineID;
    private final String name;
    private final String vendor;
    private final Kind kind;

    private TerracottaProfile(String machineID, String name, String vendor, Kind kind) {
        this.machineID = machineID;
        this.name = name;
        this.vendor = vendor;
        this.kind = kind;
    }

    public String getMachineID() { return machineID; }
    public String getName() { return name; }
    public String getVendor() { return vendor; }
    public Kind getKind() { return kind; }

    static TerracottaProfile fromJson(JsonObject obj) {
        String id = str(obj, "machine_id");
        String name = str(obj, "name");
        String vendor = str(obj, "vendor");
        Kind kind = Kind.UNKNOWN;
        String k = obj.has("kind") && obj.get("kind").isJsonPrimitive()
                ? obj.get("kind").getAsString() : null;
        if (k != null) {
            try { kind = Kind.valueOf(k.toUpperCase()); } catch (Exception ignored) {}
        }
        return new TerracottaProfile(id, name, vendor, kind);
    }

    @Nullable
    private static String str(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : null;
    }
}
