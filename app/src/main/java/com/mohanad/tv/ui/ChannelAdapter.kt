package com.mohanad.tv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mohanad.tv.R
import com.mohanad.tv.model.Channel

/**
 * Adapter خفيف جداً لعرض قائمة القنوات.
 * عمداً لا يحمّل صور اللوغو (Glide/Coil) لتوفير استهلاك البيانات
 * والحفاظ على سرعة التمرير حتى على الأجهزة الضعيفة.
 */
class ChannelAdapter(
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val fullList = mutableListOf<Channel>()
    private val visibleList = mutableListOf<Channel>()

    fun submitList(channels: List<Channel>) {
        fullList.clear()
        fullList.addAll(channels)
        visibleList.clear()
        visibleList.addAll(channels)
        notifyDataSetChanged()
    }

    /**
     * فلترة سريعة وخفيفة (بحث محلي بدون أي استدعاء شبكي) حسب الاسم أو الفئة.
     */
    fun filter(query: String, category: String?) {
        visibleList.clear()
        val lowerQuery = query.trim().lowercase()
        visibleList.addAll(
            fullList.filter { channel ->
                val matchesQuery = lowerQuery.isEmpty() || channel.name.lowercase().contains(lowerQuery)
                val matchesCategory = category == null || category == "الكل" || channel.groupTitle == category
                matchesQuery && matchesCategory
            }
        )
        notifyDataSetChanged()
    }

    fun getCategories(): List<String> {
        val categories = fullList.map { it.groupTitle }.distinct().sorted()
        return listOf("الكل") + categories
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(visibleList[position])
    }

    override fun getItemCount(): Int = visibleList.size

    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.text_channel_name)
        private val groupText: TextView = itemView.findViewById(R.id.text_channel_group)

        fun bind(channel: Channel) {
            nameText.text = channel.name
            groupText.text = channel.groupTitle
            itemView.setOnClickListener { onChannelClick(channel) }
        }
    }
}
