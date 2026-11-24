package com.applify.securepass

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.applify.securepass.data.VaultItem

class VaultAdapter(
    private val items: List<VaultItem>,
    private val listener: OnItemClickListener?,
    private val deleteListener: OnItemDeleteListener?,
    private val favoriteToggleListener: OnFavoriteToggleListener?,
    var reusedPasswords: Set<String> = emptySet()
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
        val context = holder.itemView.context
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

        // Quick Copy buttons
        holder.btnCopyPass.setOnClickListener {
            if (item.password.isNotEmpty()) {
                ClipboardUtil.copyAndClear(context, item.website, item.password, 30)
                Toast.makeText(context, R.string.toast_copied_password, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No password saved", Toast.LENGTH_SHORT).show()
            }
        }
        holder.btnCopyUser.setOnClickListener {
            if (item.username.isNotEmpty()) {
                ClipboardUtil.copyAndClear(context, item.website, item.username, 30)
                Toast.makeText(context, R.string.toast_copied_username, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No username saved", Toast.LENGTH_SHORT).show()
            }
        }

        // Password Strength Badge
        if (item.password.isNotBlank()) {
            val strength = PasswordStrengthEstimator.estimate(item.password)
            holder.tvStrengthBadge.visibility = View.VISIBLE
            holder.tvStrengthBadge.text = strength.level.label
            val color = strength.level.colorHex
            holder.tvStrengthBadge.setTextColor(color)
            val alphaBg = Color.argb(35, Color.red(color), Color.green(color), Color.blue(color))
            holder.tvStrengthBadge.backgroundTintList = ColorStateList.valueOf(alphaBg)
        } else {
            holder.tvStrengthBadge.visibility = View.GONE
        }

        // Reused Password Badge
        if (item.password.isNotBlank() && reusedPasswords.contains(item.password)) {
            holder.tvReusedBadge.visibility = View.VISIBLE
        } else {
            holder.tvReusedBadge.visibility = View.GONE
        }

        // TOTP Badge & quick copy
        if (item.totpSecret.isNotBlank()) {
            holder.tvTotpBadge.visibility = View.VISIBLE
            try {
                val code = Totp.code(item.totpSecret)
                val formatted = if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code
                holder.tvTotpBadge.text = "🔑 $formatted"
            } catch (e: Exception) {
                holder.tvTotpBadge.text = "🔑 2FA"
            }
            holder.tvTotpBadge.setOnClickListener {
                try {
                    val remaining = 30 - (System.currentTimeMillis() / 1000 % 30).toInt()
                    val code = Totp.code(item.totpSecret)
                    ClipboardUtil.copyAndClear(context, "2FA Code", code, remaining)
                    Toast.makeText(context, R.string.toast_copied_totp, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Invalid 2FA secret", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            holder.tvTotpBadge.visibility = View.GONE
            holder.tvTotpBadge.setOnClickListener(null)
        }

        // Item click: show full entry menu dialog
        holder.itemView.setOnClickListener { v ->
            val options = mutableListOf<CharSequence>(
                "Copy Password",
                "Copy Username",
                if (item.isFavorite) "Remove from Favorites" else "Mark as Favorite",
                "Edit Entry",
                "Delete"
            )
            if (item.totpSecret.isNotBlank()) options.add("Copy 2FA Authenticator Code")
            AlertDialog.Builder(v.context)
                .setTitle(item.website)
                .setItems(options.toTypedArray()) { _, which ->
                    when (which) {
                        0 -> {
                            ClipboardUtil.copyAndClear(v.context, item.website, item.password, 30)
                            Toast.makeText(v.context, R.string.toast_copied_password, Toast.LENGTH_SHORT).show()
                        }
                        1 -> {
                            ClipboardUtil.copyAndClear(v.context, item.website, item.username, 30)
                            Toast.makeText(v.context, R.string.toast_copied_username, Toast.LENGTH_SHORT).show()
                        }
                        2 -> favoriteToggleListener?.onToggle(item)
                        3 -> listener?.onItemClick(item)
                        4 -> deleteListener?.onDelete(item)
                        5 -> try {
                            val remaining = 30 - (System.currentTimeMillis() / 1000 % 30).toInt()
                            ClipboardUtil.copyAndClear(v.context, "Authenticator code", Totp.code(item.totpSecret), remaining)
                            Toast.makeText(v.context, R.string.toast_copied_totp, Toast.LENGTH_SHORT).show()
                        } catch (e: IllegalArgumentException) {
                            Toast.makeText(v.context, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val siteBadge: TextView = itemView.findViewById(R.id.tvSiteBadge)
        val siteName: TextView = itemView.findViewById(R.id.tvSiteName)
        val username: TextView = itemView.findViewById(R.id.tvUsername)
        val favorite: TextView = itemView.findViewById(R.id.tvFavorite)
        val btnCopyPass: ImageView = itemView.findViewById(R.id.btnCopyPass)
        val btnCopyUser: ImageView = itemView.findViewById(R.id.btnCopyUser)
        val tvStrengthBadge: TextView = itemView.findViewById(R.id.tvStrengthBadge)
        val tvReusedBadge: TextView = itemView.findViewById(R.id.tvReusedBadge)
        val tvTotpBadge: TextView = itemView.findViewById(R.id.tvTotpBadge)
    }
}
