package com.applify.securepass;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.applify.securepass.data.VaultItem;
import com.applify.securepass.data.VaultManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.crypto.SecretKey;

public class VaultActivity extends BaseLockActivity {

    private VaultManager vaultManager;
    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private TextInputEditText etSearch;
    private VaultAdapter adapter;
    private List<VaultItem> entries = new ArrayList<>();
    private List<VaultItem> allEntries = new ArrayList<>();
    private String userCode;   // may be null if unlocked via biometric
    private Button btnAddFirst;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        vaultManager = new VaultManager(this);

        // Bind views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerViewVault);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        etSearch = findViewById(R.id.etSearch);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        btnAddFirst = findViewById(R.id.btnAddFirst);

        // Empty-state button click -> open AddEditActivity
        btnAddFirst.setOnClickListener(v -> {
            Intent intent = new Intent(VaultActivity.this, AddEditActivity.class);
            intent.putExtra("USER_CODE", userCode);
            startActivity(intent);
        });

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VaultAdapter(entries,
                item -> {
                    // Edit entry
                    Intent intent = new Intent(VaultActivity.this, AddEditActivity.class);
                    intent.putExtra("USER_CODE", userCode);
                    intent.putExtra("ITEM_ID", item.id);
                    startActivity(intent);
                },
                item -> {
                    // Delete entry
                    deleteItem(item);
                },
                item -> {
                    // Toggle Favorite
                    item.isFavorite = !item.isFavorite;
                    saveAllAndRefresh();
                });
        recyclerView.setAdapter(adapter);

        // Search listener
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntries(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Swipe to delete
        setupSwipeToDelete();

        // Unlock the vault
        SecretKey sessionKey = VaultManager.getGlobalKey();
        if (sessionKey != null) {
            vaultManager.unlockWithKey(sessionKey);
            userCode = null;
        } else {
            userCode = getIntent().getStringExtra("USER_CODE");
            if (userCode != null) {
                try {
                    vaultManager.unlock(userCode);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Unlock failed", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                finish();
                return;
            }
        }

        // Load entries
        loadEntries();

        // FAB to add new entry
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(VaultActivity.this, AddEditActivity.class);
            intent.putExtra("USER_CODE", userCode);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEntries();   // refresh list when returning from AddEditActivity
    }

    private void loadEntries() {
        try {
            allEntries.clear();
            allEntries.addAll(vaultManager.loadEntries());
            filterEntries(etSearch.getText() != null ? etSearch.getText().toString() : "");
            toggleEmptyState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filterEntries(String query) {
        entries.clear();
        if (query.isEmpty()) {
            entries.addAll(allEntries);
        } else {
            String lowerQuery = query.toLowerCase();
            for (VaultItem item : allEntries) {
                if (item.website.toLowerCase().contains(lowerQuery) ||
                    item.username.toLowerCase().contains(lowerQuery)) {
                    entries.add(item);
                }
            }
        }
        sortEntries();
        adapter.notifyDataSetChanged();
    }

    private void sortEntries() {
        Collections.sort(entries, (a, b) -> {
            if (a.isFavorite != b.isFavorite) {
                return a.isFavorite ? -1 : 1;
            }
            return a.website.compareToIgnoreCase(b.website);
        });
    }

    private void saveAllAndRefresh() {
        try {
            if (!vaultManager.isUnlocked() && userCode != null) {
                vaultManager.unlock(userCode);
            }
            vaultManager.saveEntries(allEntries);
            loadEntries();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save changes", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteItem(VaultItem item) {
        try {
            if (!vaultManager.isUnlocked() && userCode != null) {
                vaultManager.unlock(userCode);
            }
            allEntries.removeIf(i -> i.id.equals(item.id));
            vaultManager.saveEntries(allEntries);
            loadEntries();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleEmptyState() {
        if (allEntries.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            btnAddFirst.setVisibility(View.VISIBLE);
            findViewById(R.id.tilSearch).setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            btnAddFirst.setVisibility(View.GONE);
            findViewById(R.id.tilSearch).setVisibility(View.VISIBLE);
        }
    }

    // ---------- Swipe to delete ----------
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                VaultItem item = entries.get(position);
                new AlertDialog.Builder(VaultActivity.this)
                        .setTitle("Delete")
                        .setMessage("Delete " + item.website + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            deleteItem(item);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            adapter.notifyItemChanged(position); // cancel swipe
                        })
                        .show();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    // ---------- Toolbar menu (Settings) ----------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.vault_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}