package com.applify.securepass

import android.R
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.applify.securepass.data.VaultItem

class VaultAdapter(
    private val items: List<VaultItem>,
    private val listener: OnItemClickListener?,
    private val deleteListener: OnItemDeleteListener?,
    private val favoriteToggleListener: OnFavoriteToggleListener?
) : RecyclerView.Adapter<VaultAdapter.ViewHolder>() {

    fun interface OnItemClickListener {
        fun onItemClick(item: VaultItem)
    }

    fun interface OnItemDeleteListener {
        fun onDelete(item: VaultItem)
    }

    fun interface OnFavoriteToggleListener {
        fun onToggle(item: VaultItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.text1.text = (if (item.isFavorite) "★ " else "☆ ") + item.website
        holder.text2.text = item.username

        // Item click: show action dialog
        holder.itemView.setOnClickListener { v ->
            val options = arrayOf<CharSequence>(
                "Copy Password",
                if (item.isFavorite) "Remove from Favorites" else "Mark as Favorite",
                "Edit",
                "Delete"
            )
            AlertDialog.Builder(v.context)
                .setTitle(item.website)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> ClipboardUtil.copyAndClear(v.context, item.website, item.password, 30)
                        1 -> favoriteToggleListener?.onToggle(item)
                        2 -> listener?.onItemClick(item)
                        3 -> deleteListener?.onDelete(item)
                    }
                }
                .show()
        }

        // Remove long-press listener - now delete is in the dialog
        holder.itemView.setOnLongClickListener(null)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text1: TextView = itemView.findViewById(R.id.text1)
        val text2: TextView = itemView.findViewById(R.id.text2)
    }
}