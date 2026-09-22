package com.applify.securepass

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.applify.securepass.data.VaultItem
import com.applify.securepass.data.VaultManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.util.ArrayList
import java.util.Objects
import javax.crypto.SecretKey

class VaultActivity : BaseLockActivity() {

    private lateinit var vaultManager: VaultManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var etSearch: TextInputEditText
    private lateinit var adapter: VaultAdapter
    private val entries: MutableList<VaultItem> = ArrayList()
    private val allEntries: MutableList<VaultItem> = ArrayList()
    private var userCode: String? = null // may be null if unlocked via biometric
    private lateinit var btnAddFirst: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault)

        vaultManager = VaultManager(this)

        // Bind views
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerViewVault)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        etSearch = findViewById(R.id.etSearch)
        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)
        btnAddFirst = findViewById(R.id.btnAddFirst)

        // Empty-state button click -> open AddEditActivity
        btnAddFirst.setOnClickListener {
            val intent = Intent(this@VaultActivity, AddEditActivity::class.java)
            intent.putExtra("USER_CODE", userCode)
            startActivity(intent)
        }

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = VaultAdapter(
            entries,
            { item ->
                // Edit entry
                val intent = Intent(this@VaultActivity, AddEditActivity::class.java)
                intent.putExtra("USER_CODE", userCode)
                intent.putExtra("ITEM_ID", item.id)
                startActivity(intent)
            },
            { item -> deleteItem(item) },
            { item ->
                // Toggle Favorite
                item.isFavorite = !item.isFavorite
                saveAllAndRefresh()
            }
        )
        recyclerView.adapter = adapter

        // Search listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterEntries(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Swipe to delete
        setupSwipeToDelete()

        // Unlock the vault
        val sessionKey: SecretKey? = VaultManager.globalKey
        if (sessionKey != null) {
            vaultManager.unlockWithKey(sessionKey)
            userCode = null
        } else {
            userCode = intent.getStringExtra("USER_CODE")
            val code = userCode
            if (code != null) {
                try {
                    vaultManager.unlock(code)
                } catch (e: Exception) {
                    Log.e(TAG, "Unlock failed", e)
                    Toast.makeText(this, "Unlock failed", Toast.LENGTH_SHORT).show();
                    finish()
                    return
                }
            } else {
                finish()
                return
            }
        }

        // Load entries
        loadEntries()

        // FAB to add new entry
        fabAdd.setOnClickListener {
            val intent = Intent(this@VaultActivity, AddEditActivity::class.java)
            intent.putExtra("USER_CODE", userCode)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadEntries() // refresh list when returning from AddEditActivity
    }

    private fun loadEntries() {
        try {
            allEntries.clear()
            allEntries.addAll(vaultManager.loadEntries())
            filterEntries(etSearch.text?.toString() ?: "")
            toggleEmptyState()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading entries", e)
        }
    }

    private fun filterEntries(query: String) {
        entries.clear()
        if (query.isEmpty()) {
            entries.addAll(allEntries)
        } else {
            val lowerQuery = query.lowercase()
            for (item in allEntries) {
                if (item.website.lowercase().contains(lowerQuery) ||
                    item.username.lowercase().contains(lowerQuery)
                ) {
                    entries.add(item)
                }
            }
        }
        sortEntries()
        adapter.notifyDataSetChanged()
    }

    private fun sortEntries() {
        entries.sortWith { a, b ->
            if (a.isFavorite != b.isFavorite) {
                if (a.isFavorite) -1 else 1
            } else {
                a.website.compareTo(b.website, ignoreCase = true)
            }
        }
    }

    private fun saveAllAndRefresh() {
        try {
            val code = userCode
            if (!vaultManager.isUnlocked() && code != null) {
                vaultManager.unlock(code)
            }
            vaultManager.saveEntries(allEntries)
            loadEntries()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save changes", e)
            Toast.makeText(this, "Failed to save changes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteItem(item: VaultItem) {
        try {
            val code = userCode
            if (!vaultManager.isUnlocked() && code != null) {
                vaultManager.unlock(code)
            }
            allEntries.removeAll { i -> Objects.equals(i.id, item.id) }
            vaultManager.saveEntries(allEntries)
            loadEntries()
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed", e)
            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleEmptyState() {
        if (allEntries.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            btnAddFirst.visibility = View.VISIBLE
            findViewById<View>(R.id.tilSearch).visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            btnAddFirst.visibility = View.GONE
            findViewById<View>(R.id.tilSearch).visibility = View.VISIBLE
        }
    }

    // ---------- Swipe actions ----------
    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val paint = Paint()
                    val alpha = (Math.min(Math.abs(dX) / itemView.width, 1.0f) * 255).toInt()

                    if (dX > 0) { // Swipe Right
                        val rightAction = getSharedPreferences("secure_pass_prefs", MODE_PRIVATE).getInt("swipe_right_action", 1)
                        if (rightAction == 1) {
                            paint.color = Color.argb(alpha, 211, 47, 47) // Red for Delete
                        } else if (rightAction == 2) {
                            paint.color = Color.argb(alpha, 255, 179, 0) // Amber for Favorite
                        } else {
                            paint.color = Color.TRANSPARENT
                        }
                        c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat(), paint)
                    } else if (dX < 0) { // Swipe Left
                        val leftAction = getSharedPreferences("secure_pass_prefs", MODE_PRIVATE).getInt("swipe_left_action", 2)
                        if (leftAction == 1) {
                            paint.color = Color.argb(alpha, 211, 47, 47) // Red for Delete
                        } else if (leftAction == 2) {
                            paint.color = Color.argb(alpha, 255, 179, 0) // Amber for Favorite
                        } else {
                            paint.color = Color.TRANSPARENT
                        }
                        c.drawRect(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), paint)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position < 0 || position >= entries.size) return
                val item = entries[position]

                val prefs = getSharedPreferences("secure_pass_prefs", MODE_PRIVATE)
                val action = if (direction == ItemTouchHelper.RIGHT) prefs.getInt("swipe_right_action", 1) else prefs.getInt("swipe_left_action", 2)

                if (action == 1) { // Delete
                    AlertDialog.Builder(this@VaultActivity)
                        .setTitle("Delete")
                        .setMessage("Delete " + item.website + "?")
                        .setPositiveButton("Delete") { _, _ -> deleteItem(item) }
                        .setNegativeButton("Cancel") { _, _ -> adapter.notifyItemChanged(position) }
                        .show()
                } else if (action == 2) { // Favorite
                    item.isFavorite = !item.isFavorite
                    saveAllAndRefresh()
                } else { // None
                    adapter.notifyItemChanged(position)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    // ---------- Toolbar menu (Settings) ----------
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.vault_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "VaultActivity"
    }
}