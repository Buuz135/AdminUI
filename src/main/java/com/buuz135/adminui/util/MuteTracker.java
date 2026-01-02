package com.buuz135.adminui.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.hypixel.hytale.server.core.util.io.BlockingDiskFile;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MuteTracker extends BlockingDiskFile {

    private List<MuteTracker.Mute> muteTracker;

    public MuteTracker() {
        super(Path.of("AdminUI/Mute.json"));
        this.muteTracker = new ArrayList<>();
    }

    @Override
    protected void read(BufferedReader bufferedReader) throws IOException {
        JsonParser.parseReader(bufferedReader).getAsJsonArray().forEach((entry) -> {
            JsonObject object = entry.getAsJsonObject();
            try {
                this.muteTracker.add(new Mute(UUID.fromString(object.get("target").getAsString()), UUID.fromString(object.get("mutedBy").getAsString()), Instant.ofEpochMilli(object.get("until").getAsLong()), object.get("reason").getAsString()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse player tracker!", e);
            }
        });
    }

    public void addMute(Mute mute){
        this.fileLock.writeLock().lock();
        this.muteTracker.removeIf(player -> player.target().equals(mute.target()));
        this.muteTracker.add(mute);
        this.fileLock.writeLock().unlock();
        this.syncSave();
    }

    @Override
    protected void write(BufferedWriter bufferedWriter) throws IOException {
        JsonArray array = new JsonArray();
        this.getMutes().forEach(mute -> {
            var object = new JsonObject();
            object.addProperty("target", mute.target().toString());
            object.addProperty("mutedBy", mute.mutedBy().toString());
            object.addProperty("until", mute.until().toEpochMilli());
            object.addProperty("reason", mute.reason());
            array.add(object);
        });
        bufferedWriter.write(array.toString());
    }

    public List<Mute> getMutes() {
        if (this.muteTracker.removeIf(mute -> mute.until().isBefore(Instant.now()))) {
            this.syncSave();
        }
        return muteTracker;
    }

    @Nullable
    public MuteTracker.Mute getPlayer(UUID uuid){
        return this.getMutes().stream().filter(mute -> mute.target().equals(uuid)).findFirst().orElse(null);
    }

    public boolean isMuted(UUID uuid){
        var playerMute = this.getPlayer(uuid);
        if (playerMute == null) return false;
        return playerMute.until().isAfter(Instant.now());
    }

    @Override
    protected void create(BufferedWriter bufferedWriter) throws IOException {
        try (JsonWriter jsonWriter = new JsonWriter(bufferedWriter)) {
            jsonWriter.beginArray().endArray();
        }
    }

    public record Mute(UUID target, UUID mutedBy, Instant until, String reason) {

    }
}
