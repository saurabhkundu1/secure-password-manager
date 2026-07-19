package com.applify.securepass;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;

public class VaultActivity extends AppCompatActivity {

    private VaultManager vaultManager;
    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private VaultAdapter adapter;
    private List<VaultItem> entries = new ArrayList<>();
    private String userCode;   // may be null if unlocked via biometric
    private Button btnAddFirst;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        vaultManager = new VaultManager(this);

        // Bind views
        recyclerView = findViewById(R.id.recyclerViewVault);
        layoutEmpty = findViewById(R.id.layoutEmpty);
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
                    // Delete entry (used by dialog and long‑press in adapter)
                    try {
                        if (!vaultManager.isUnlocked() && userCode != null) {
                            vaultManager.unlock(userCode);
                        }
                        List<VaultItem> list = vaultManager.loadEntries();
                        list.removeIf(i -> i.id.equals(item.id));
                        vaultManager.saveEntries(list);
                        loadEntries();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(VaultActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                });
        recyclerView.setAdapter(adapter);

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
            entries.clear();
            entries.addAll(vaultManager.loadEntries());
            adapter.notifyDataSetChanged();
            toggleEmptyState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleEmptyState() {
        if (entries.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            btnAddFirst.setVisibility(View.VISIBLE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            btnAddFirst.setVisibility(View.GONE);
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
                            try {
                                if (!vaultManager.isUnlocked() && userCode != null) {
                                    vaultManager.unlock(userCode);
                                }
                                List<VaultItem> list = vaultManager.loadEntries();
                                list.removeIf(i -> i.id.equals(item.id));
                                vaultManager.saveEntries(list);
                                loadEntries();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(VaultActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                                loadEntries(); // refresh to revert swipe visually
                            }
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