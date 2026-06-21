package com.v2ray.ang.ui

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.databinding.ItemRecyclerSubSettingBinding
import com.v2ray.ang.dto.AngConfig
import com.v2ray.ang.util.AngConfigManager

class SubSettingRecyclerAdapter(private val activity: SubSettingActivity) : 
    RecyclerView.Adapter<SubSettingRecyclerAdapter.MainViewHolder>() {

    private var configs: AngConfig = AngConfigManager.configs

    init {
        updateConfigList()
    }

    override fun getItemCount() = configs.subItem.count()

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val item = configs.subItem[position]
        
        holder.binding.tvName.text = item.remarks
        holder.binding.tvUrl.text = item.url
        holder.itemView.setBackgroundColor(Color.TRANSPARENT)

        holder.binding.layoutEdit.setOnClickListener {
            activity.startActivity(Intent(activity, SubEditActivity::class.java)
                .putExtra("position", position)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = ItemRecyclerSubSettingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MainViewHolder(binding)
    }

    fun updateConfigList() {
        configs = AngConfigManager.configs
        notifyDataSetChanged()
    }

    class MainViewHolder(val binding: ItemRecyclerSubSettingBinding) : 
        RecyclerView.ViewHolder(binding.root)
}
