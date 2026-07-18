package com.applify.securepass;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
    private String userCode;
    private Button btnAddFirst;   // <-- for empty state

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        vaultManager = new VaultManager(this);

        // Bind views
        recyclerView = findViewById(R.id.recyclerViewVault);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        btnAddFirst = findViewById(R.id.btnAddFirst);   // <-- bind the empty-state button

        // Empty-state button click → open AddEditActivity
        btnAddFirst.setOnClickListener(v -> {
            Intent intent = new Intent(VaultActivity.this, AddEditActivity.class);
            intent.putExtra("USER_CODE", userCode);
            startActivity(intent);
        });

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VaultAdapter(entries,
                item -> {
                    Intent intent = new Intent(VaultActivity.this, AddEditActivity.class);
                    intent.putExtra("USER_CODE", userCode);
                    intent.putExtra("ITEM_ID", item.id);
                    startActivity(intent);
                },
                item -> {
                    try {
                        vaultManager.unlock(userCode);
                        List<VaultItem> list = vaultManager.loadEntries();
                        list.removeIf(i -> i.id.equals(item.id));
                        vaultManager.saveEntries(list);
                        loadEntries();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                });
        recyclerView.setAdapter(adapter);

        // Try biometric key first, then fall back to user code
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

        // Load entries from vault
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
        loadEntries();
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
            btnAddFirst.setVisibility(View.VISIBLE);   // show the big button
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            btnAddFirst.setVisibility(View.GONE);      // hide it when list is shown
        }
    }
}