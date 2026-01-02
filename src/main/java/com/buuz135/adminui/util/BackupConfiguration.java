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

public class BackupConfiguration extends BlockingDiskFile {

    private boolean enabled;
    private String folder;
    private int retentionAmount;
    private int backupFrequency;

    public BackupConfiguration() {
        super(Path.of("AdminUI/Backup.json"));
        this.enabled = true;
        this.folder = "backups";
        this.retentionAmount = 10;
        this.backupFrequency = 60;
    }

    @Override
    protected void read(BufferedReader bufferedReader) throws IOException {
        var object = JsonParser.parseReader(bufferedReader).getAsJsonObject();
        this.enabled = object.get("enabled").getAsBoolean();
        this.folder = object.get("folder").getAsString();
        this.retentionAmount = object.get("retentionAmount").getAsInt();
        this.backupFrequency = object.get("backupFrequency").getAsInt();
    }

    @Override
    protected void write(BufferedWriter bufferedWriter) throws IOException {
        JsonObject object = new JsonObject();
        object.addProperty("enabled", this.enabled);
        object.addProperty("folder", this.folder);
        object.addProperty("retentionAmount", this.retentionAmount);
        object.addProperty("backupFrequency", this.backupFrequency);
        bufferedWriter.write(object.toString());
    }


    @Override
    protected void create(BufferedWriter bufferedWriter) throws IOException {
        write(bufferedWriter);
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public int getRetentionAmount() {
        return retentionAmount;
    }

    public void setRetentionAmount(int retentionAmount) {
        this.retentionAmount = retentionAmount;
    }

    public int getBackupFrequency() {
        return backupFrequency;
    }

    public void setBackupFrequency(int backupFrequency) {
        this.backupFrequency = backupFrequency;
    }
}
