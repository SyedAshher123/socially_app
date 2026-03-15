package com.example.i230657_i230007

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class FollowRequestAdapter(
    private val requests: List<Notification>,
    private val onAccept: (Notification) -> Unit,
    private val onReject: (Notification) -> Unit
) : RecyclerView.Adapter<FollowRequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.default_profile)
        val messageText: TextView = itemView.findViewById(R.id.tvDisplayName)
        val btnAccept: MaterialButton = itemView.findViewById(R.id.btnAccept)
        val btnReject: MaterialButton = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val notif = requests[position]
        val username = notif.fromUsername.takeIf { it.isNotEmpty() } ?: notif.fromUserId
        holder.messageText.text = "$username sent you a follow request!"

        val pfp = notif.fromProfilePicture
        if (!pfp.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(pfp, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.profileImage.setImageBitmap(bmp)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
        }

        holder.btnAccept.setOnClickListener { onAccept(notif) }
        holder.btnReject.setOnClickListener { onReject(notif) }
    }

    override fun getItemCount(): Int = requests.size
}
