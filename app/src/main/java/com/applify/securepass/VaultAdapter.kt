package com.applify.securepass

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
            .inflate(R.layout.item_vault, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.siteName.text = item.website
        holder.username.text = item.username.ifBlank { "No username saved" }
        holder.siteBadge.text = item.website.firstOrNull()?.uppercaseChar()?.toString() ?: "•"
        holder.favorite.text = if (item.isFavorite) "★" else "☆"
        holder.favorite.contentDescription = if (item.isFavorite) {
            "Remove ${item.website} from favorites"
        } else {
            "Add ${item.website} to favorites"
        }
        holder.favorite.setOnClickListener { favoriteToggleListener?.onToggle(item) }

        // Item click: show action dialog
        holder.itemView.setOnClickListener { v ->
            val options = mutableListOf<CharSequence>(
                "Copy Password",
                if (item.isFavorite) "Remove from Favorites" else "Mark as Favorite",
                "Edit",
                "Delete"
            )
            if (item.totpSecret.isNotBlank()) options.add("Copy authenticator code")
            AlertDialog.Builder(v.context)
                .setTitle(item.website)
                .setItems(options.toTypedArray()) { _, which ->
                    when (which) {
                        0 -> ClipboardUtil.copyAndClear(v.context, item.website, item.password, 30)
                        1 -> favoriteToggleListener?.onToggle(item)
                        2 -> listener?.onItemClick(item)
                        3 -> deleteListener?.onDelete(item)
                        4 -> try {
                            val remaining = 30 - (System.currentTimeMillis() / 1000 % 30).toInt()
                            ClipboardUtil.copyAndClear(v.context, "Authenticator code", Totp.code(item.totpSecret), remaining)
                        } catch (e: IllegalArgumentException) {
                            android.widget.Toast.makeText(v.context, e.message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
        }

        // Remove long-press listener - now delete is in the dialog
        holder.itemView.setOnLongClickListener(null)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val siteBadge: TextView = itemView.findViewById(R.id.tvSiteBadge)
        val siteName: TextView = itemView.findViewById(R.id.tvSiteName)
        val username: TextView = itemView.findViewById(R.id.tvUsername)
        val favorite: TextView = itemView.findViewById(R.id.tvFavorite)
    }
}
