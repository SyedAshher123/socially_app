package com.yourapp.adapters

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.i230657_i230007.R
import com.example.i230657_i230007.post_view_page
import com.yourapp.models.Message
import de.hdodenhof.circleimageview.CircleImageView

class MessageAdapter(
    private val messageList: MutableList<Message>,
    private val currentUserId: String,                // ✅ NEW — passed from chat_page.kt
    private val onMessageLongClick: (Message) -> Unit // long press callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val MSG_TYPE_SENT = 1
    private val MSG_TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == currentUserId)
            MSG_TYPE_SENT
        else
            MSG_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == MSG_TYPE_SENT) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentVH(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedVH(v)
        }
    }

    override fun getItemCount(): Int = messageList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        val msg = messageList[pos]

        if (holder is SentVH) holder.bind(msg)
        else if (holder is ReceivedVH) holder.bind(msg)

        holder.itemView.setOnLongClickListener {
            onMessageLongClick(msg)
            true
        }
    }

    /////////////////////////////////////////////
    // SENT VIEW HOLDER
    /////////////////////////////////////////////

    inner class SentVH(item: View) : RecyclerView.ViewHolder(item) {

        private val txt: TextView = item.findViewById(R.id.text_message_sent)
        private val time: TextView = item.findViewById(R.id.text_message_time_sent)
        private val img: ImageView = item.findViewById(R.id.image_message_sent)

        private val box: ViewGroup? = item.findViewById(R.id.postItemContainer)
        private val postImg: ImageView? = item.findViewById(R.id.vpPostImages)
        private val postPfp: CircleImageView? = item.findViewById(R.id.postProfileImage)
        private val postCap: TextView? = item.findViewById(R.id.postCaption)
        private val postUser: TextView? = item.findViewById(R.id.tvUsername)

        fun bind(m: Message) {

            when (m.type) {
                "text" -> showText(m)
                "image" -> showImage(m)
                "post" -> showPost(m)
            }

            time.text = format(m.timestamp)
        }

        private fun showText(m: Message) {
            txt.visibility = View.VISIBLE
            img.visibility = View.GONE
            box?.visibility = View.GONE
            txt.text = m.messageText
        }

        private fun showImage(m: Message) {
            txt.visibility = View.GONE
            img.visibility = View.VISIBLE
            box?.visibility = View.GONE

            try {
                val clean = m.imageUrl.trim()
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                img.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (_: Exception) {
                img.setImageResource(R.drawable.placeholder_pfp)
            }
        }

        private fun showPost(m: Message) {
            txt.visibility = View.GONE
            img.visibility = View.GONE
            box?.visibility = View.VISIBLE

            m.post?.let { p ->

                try {
                    // Profile picture
                    val pp = Base64.decode(p.userProfilePicture.trim(), Base64.DEFAULT)
                    postPfp?.setImageBitmap(BitmapFactory.decodeByteArray(pp, 0, pp.size))

                    // Post image
                    val pi = Base64.decode(p.postImage.trim(), Base64.DEFAULT)
                    postImg?.setImageBitmap(BitmapFactory.decodeByteArray(pi, 0, pi.size))

                } catch (_: Exception) {
                    postPfp?.setImageResource(R.drawable.placeholder_pfp)
                }

                postCap?.text = p.postCaption
                postUser?.text = p.userId

                box?.setOnClickListener {
                    val i = Intent(itemView.context, post_view_page::class.java)
                    i.putExtra("postId", p.postId)
                    itemView.context.startActivity(i)
                }
            }
        }
    }

    /////////////////////////////////////////////
    // RECEIVED VIEW HOLDER
    /////////////////////////////////////////////

    inner class ReceivedVH(item: View) : RecyclerView.ViewHolder(item) {

        private val txt: TextView = item.findViewById(R.id.text_message_received)
        private val time: TextView = item.findViewById(R.id.text_message_time_received)
        private val img: ImageView = item.findViewById(R.id.image_message_received)

        private val box: ViewGroup? = item.findViewById(R.id.postItemContainer)
        private val postImg: ImageView? = item.findViewById(R.id.vpPostImages)
        private val postPfp: CircleImageView? = item.findViewById(R.id.postProfileImage)
        private val postCap: TextView? = item.findViewById(R.id.postCaption)
        private val postUser: TextView? = item.findViewById(R.id.tvUsername)

        fun bind(m: Message) {

            when (m.type) {
                "text" -> showText(m)
                "image" -> showImage(m)
                "post" -> showPost(m)
            }

            time.text = format(m.timestamp)
        }

        private fun showText(m: Message) {
            txt.visibility = View.VISIBLE
            img.visibility = View.GONE
            box?.visibility = View.GONE
            txt.text = m.messageText
        }

        private fun showImage(m: Message) {
            txt.visibility = View.GONE
            img.visibility = View.VISIBLE
            box?.visibility = View.GONE

            try {
                val clean = m.imageUrl.trim()
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                img.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (_: Exception) {
                img.setImageResource(R.drawable.placeholder_pfp)
            }
        }

        private fun showPost(m: Message) {
            txt.visibility = View.GONE
            img.visibility = View.GONE
            box?.visibility = View.VISIBLE

            m.post?.let { p ->

                try {
                    val pp = Base64.decode(p.userProfilePicture.trim(), Base64.DEFAULT)
                    postPfp?.setImageBitmap(BitmapFactory.decodeByteArray(pp, 0, pp.size))

                    val pi = Base64.decode(p.postImage.trim(), Base64.DEFAULT)
                    postImg?.setImageBitmap(BitmapFactory.decodeByteArray(pi, 0, pi.size))

                } catch (_: Exception) {
                    postPfp?.setImageResource(R.drawable.placeholder_pfp)
                }

                postCap?.text = p.postCaption
                postUser?.text = p.userId
            }
        }
    }

    /////////////////////////////////////////////
    // TIME FORMAT
    /////////////////////////////////////////////

    private fun format(t: Long): String {
        val d = java.util.Date(t)
        val f = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return f.format(d)
    }
}
