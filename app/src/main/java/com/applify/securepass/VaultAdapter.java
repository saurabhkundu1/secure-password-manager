package com.applify.securepass;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.applify.securepass.data.VaultItem;
import java.util.List;

public class VaultAdapter extends RecyclerView.Adapter<VaultAdapter.ViewHolder> {

    public interface OnItemClickListener { void onItemClick(VaultItem item); }
    public interface OnItemDeleteListener { void onDelete(VaultItem item); }

    private List<VaultItem> items;
    private OnItemClickListener listener;
    private OnItemDeleteListener deleteListener;

    public VaultAdapter(List<VaultItem> items, OnItemClickListener listener,
                        OnItemDeleteListener deleteListener) {
        this.items = items;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VaultItem item = items.get(position);
        holder.text1.setText(item.website);
        holder.text2.setText(item.username);

        // Tap: copy password to clipboard (auto‑clears)
        holder.itemView.setOnClickListener(v -> {
            ClipboardUtil.copyAndClear(v.getContext(), item.website, item.password, 30);
        });

        // Long‑press: delete entry (confirmation dialog)
        holder.itemView.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Entry")
                    .setMessage("Delete " + item.website + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (deleteListener != null) deleteListener.onDelete(item);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        ViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}