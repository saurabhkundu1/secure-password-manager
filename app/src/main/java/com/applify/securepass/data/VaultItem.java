package com.applify.securepass.data;

import java.util.UUID;

/**
 * Represents one saved password entry.
 * All fields are public for simplicity (or you can use getters/setters).
 */
public class VaultItem {
    public String id;           // Unique ID for each entry
    public String website;      // Website or app name
    public String username;     // Login / email
    public String password;     // The actual password (stored encrypted in file)
    public String notes;        // User notes, security questions, etc.
    public boolean isFavorite = false;
    public long createdAt;      // Timestamp when entry was created
    public long lastChanged;    // Timestamp when entry was last modified

    // Empty constructor required for Gson
    public VaultItem() { }

    // Convenience constructor
    public VaultItem(String website, String username, String password, String notes) {
        this.id = UUID.randomUUID().toString();
        this.website = website;
        this.username = username;
        this.password = password;
        this.notes = notes;
        this.createdAt = System.currentTimeMillis();
        this.lastChanged = this.createdAt;
    }
}