package com.reprush.app.ui.member.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reprush.app.R
import com.reprush.app.data.local.entity.ChatMessageEntity

class ChatAdapter : ListAdapter<ChatMessageEntity, ChatAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutUser: View = view.findViewById(R.id.layout_userMessage)
        val layoutAi: View = view.findViewById(R.id.layout_aiMessage)
        val textUser: TextView = view.findViewById(R.id.textView_userContent)
        val textAi: TextView = view.findViewById(R.id.textView_aiContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = getItem(position)
        if (msg.role == "user") {
            holder.layoutUser.visibility = View.VISIBLE
            holder.layoutAi.visibility = View.GONE
            holder.textUser.text = msg.content
        } else {
            holder.layoutUser.visibility = View.GONE
            holder.layoutAi.visibility = View.VISIBLE
            holder.textAi.text = msg.content
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ChatMessageEntity>() {
        override fun areItemsTheSame(a: ChatMessageEntity, b: ChatMessageEntity) = a.id == b.id
        override fun areContentsTheSame(a: ChatMessageEntity, b: ChatMessageEntity) = a == b
    }
}
