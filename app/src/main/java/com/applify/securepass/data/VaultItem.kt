package com.applify.securepass.data

import java.util.UUID

/**
 * Represents one saved password entry.
 */
class VaultItem {
    @JvmField var id: String = ""
    @JvmField var website: String = ""
    @JvmField var username: String = ""
    @JvmField var password: String = ""
    @JvmField var notes: String = ""
    @JvmField var isFavorite: Boolean = false
    @JvmField var createdAt: Long = 0L
    @JvmField var lastChanged: Long = 0L

    // Empty constructor for Gson
    constructor()

    // Convenience constructor
    constructor(website: String, username: String, password: String, notes: String) {
        this.id = UUID.randomUUID().toString()
        this.website = website
        this.username = username
        this.password = password
        this.notes = notes
        this.createdAt = System.currentTimeMillis()
        this.lastChanged = this.createdAt
    }
}