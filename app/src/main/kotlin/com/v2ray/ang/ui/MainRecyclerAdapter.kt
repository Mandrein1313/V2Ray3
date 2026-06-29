package com.v2ray.ang.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.R
import com.v2ray.ang.dto.AngConfig.VmessBean
import com.v2ray.ang.helper.ItemTouchHelperAdapter
import com.v2ray.ang.util.AngConfigManager

class MainRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<MainRecyclerAdapter.ViewHolder>(),
    ItemTouchHelperAdapter {

    private var data: List<VmessBean> = AngConfigManager.configs.vmess
    var selectedIndex: Int = AngConfigManager.configs.index
    var changeable = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_server, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.tvRemarks.text = item.remarks
        holder.tvAddress.text = "${item.address}:${item.port}"
        holder.tvType.text = item.network
        holder.tvSecurity.text = item.streamSecurity
        holder.tvAlterId.text = "aid: ${item.alterId}"

        if (selectedIndex == position) {
            holder.imgCheck.visibility = View.VISIBLE
        } else {
            holder.imgCheck.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (changeable) {
                val old = selectedIndex
                selectedIndex = position
                notifyItemChanged(old)
                notifyItemChanged(position)
                AngConfigManager.setActiveServer(position)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (changeable) {
                val intent = android.content.Intent(context, ServerActivity::class.java)
                intent.putExtra("position", position)
                context.startActivity(intent)
            }
            true
        }
    }

    override fun getItemCount(): Int = data.size

    fun updateConfigList() {
        data = AngConfigManager.configs.vmess
        selectedIndex = AngConfigManager.configs.index
        notifyDataSetChanged()
    }

    fun updateSelectedItem(index: Int) {
        selectedIndex = index
        notifyDataSetChanged()
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (AngConfigManager.swapServer(fromPosition, toPosition) == 0) {
            val list = data.toMutableList()
            val item = list.removeAt(fromPosition)
            list.add(toPosition, item)
            data = list

            if (selectedIndex == fromPosition) {
                selectedIndex = toPosition
            } else if (selectedIndex == toPosition) {
                selectedIndex = fromPosition
            }
            notifyItemMoved(fromPosition, toPosition)
        }
    }

    override fun onItemDismiss(position: Int) {
        // ไม่ได้ใช้
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRemarks: TextView = itemView.findViewById(R.id.tv_remarks)
        val tvAddress: TextView = itemView.findViewById(R.id.tv_address)
        val tvType: TextView = itemView.findViewById(R.id.tv_type)
        val tvSecurity: TextView = itemView.findViewById(R.id.tv_security)
        val tvAlterId: TextView = itemView.findViewById(R.id.tv_alter_id)
        val imgCheck: ImageView = itemView.findViewById(R.id.img_check)
    }
}
