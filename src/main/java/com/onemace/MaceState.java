package com.onemace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

/**
 * Everything the mod needs to remember about "the" Mace, in one place.
 * <p>
 * Deliberately NOT using Minecraft's own PersistentState/NBT system here:
 * that API is Codec-based and shifts between versions, and this mod can't be
 * compile-tested against a real Minecraft jar before you build it. A plain
 * key=value file next to the world save is a couple of hundred bytes, easy
 * to hand-edit if something ever looks wrong, and won't break just because
 * a future patch changes Mojang's save-data internals.
 */
public class MaceState {

    /** Has a Mace been forged and not yet destroyed? */
    public boolean exists = false;

    /** Player currently holding it in their inventory/offhand, or null if it's on the ground/unheld. */
    public UUID holderUuid = null;
    public String holderName = "";

    /** If it's a loose item on the ground, which entity (by UUID) and roughly where. */
    public UUID groundEntityUuid = null;
    public String groundDimension = "";
    public double groundX, groundY, groundZ;

    /** Last place we knew a holder to be - used to search for it right after it's dropped. */
    public UUID lastKnownHolderUuid = null;
    public String lastKnownDimension = "";
    public double lastKnownX, lastKnownY, lastKnownZ;

    /** Consecutive "couldn't find it anywhere" scans, used to avoid a false "destroyed" on one bad tick. */
    public int missCounter = 0;

    public void clearToDestroyed() {
        this.exists = false;
        this.holderUuid = null;
        this.holderName = "";
        this.groundEntityUuid = null;
        this.groundDimension = "";
        this.missCounter = 0;
    }

    public void save(Path file) {
        Properties props = new Properties();
        props.setProperty("exists", Boolean.toString(exists));
        props.setProperty("holderUuid", holderUuid == null ? "" : holderUuid.toString());
        props.setProperty("holderName", holderName == null ? "" : holderName);
        props.setProperty("groundEntityUuid", groundEntityUuid == null ? "" : groundEntityUuid.toString());
        props.setProperty("groundDimension", groundDimension == null ? "" : groundDimension);
        props.setProperty("groundX", Double.toString(groundX));
        props.setProperty("groundY", Double.toString(groundY));
        props.setProperty("groundZ", Double.toString(groundZ));
        props.setProperty("lastKnownHolderUuid", lastKnownHolderUuid == null ? "" : lastKnownHolderUuid.toString());
        props.setProperty("lastKnownDimension", lastKnownDimension == null ? "" : lastKnownDimension);
        props.setProperty("lastKnownX", Double.toString(lastKnownX));
        props.setProperty("lastKnownY", Double.toString(lastKnownY));
        props.setProperty("lastKnownZ", Double.toString(lastKnownZ));
        props.setProperty("missCounter", Integer.toString(missCounter));

        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file);
                 Writer writer = new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                props.store(writer, "OneMace mod state - safe to delete to reset the mod (a new Mace can then be forged)");
            }
        } catch (IOException e) {
            OneMaceMod.LOGGER.error("[OneMace] Failed to save state to {}", file, e);
        }
    }

    public static MaceState load(Path file) {
        MaceState state = new MaceState();
        if (!Files.exists(file)) {
            return state;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file);
             Reader reader = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            OneMaceMod.LOGGER.error("[OneMace] Failed to load state from {}, starting fresh", file, e);
            return state;
        }

        state.exists = Boolean.parseBoolean(props.getProperty("exists", "false"));
        state.holderUuid = parseUuid(props.getProperty("holderUuid", ""));
        state.holderName = props.getProperty("holderName", "");
        state.groundEntityUuid = parseUuid(props.getProperty("groundEntityUuid", ""));
        state.groundDimension = props.getProperty("groundDimension", "");
        state.groundX = parseDouble(props.getProperty("groundX", "0"));
        state.groundY = parseDouble(props.getProperty("groundY", "0"));
        state.groundZ = parseDouble(props.getProperty("groundZ", "0"));
        state.lastKnownHolderUuid = parseUuid(props.getProperty("lastKnownHolderUuid", ""));
        state.lastKnownDimension = props.getProperty("lastKnownDimension", "");
        state.lastKnownX = parseDouble(props.getProperty("lastKnownX", "0"));
        state.lastKnownY = parseDouble(props.getProperty("lastKnownY", "0"));
        state.lastKnownZ = parseDouble(props.getProperty("lastKnownZ", "0"));
        state.missCounter = parseInt(props.getProperty("missCounter", "0"));
        return state;
    }

    private static UUID parseUuid(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
