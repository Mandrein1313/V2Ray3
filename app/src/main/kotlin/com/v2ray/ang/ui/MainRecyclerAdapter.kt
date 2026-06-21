package com.v2ray.ang.ui

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.R
import com.v2ray.ang.extension.defaultDPreference
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.Utils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class MainRecyclerAdapter(private val context: Context) : RecyclerView.Adapter<MainRecyclerAdapter.ViewHolder>() {

    private var data = AngConfigManager.configs.vmess
    private var selectedIndex = AngConfigManager.configs.index
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

        // การเลือก active
        if (selectedIndex == position) {
            holder.imgCheck.visibility = View.VISIBLE
        } else {
            holder.imgCheck.visibility = View.GONE
        }

        // คลิกเพื่อสลับ active
        holder.itemView.setOnClickListener {
            if (changeable) {
                val old = selectedIndex
                selectedIndex = position
                notifyItemChanged(old)
                notifyItemChanged(position)
                AngConfigManager.setActiveServer(position)
            }
        }

        // คลิกขวาเพื่อแก้ไข
        holder.itemView.setOnLongClickListener {
            if (changeable) {
                val intent = Intent(context, ServerActivity::class.java).apply {
                    putExtra("position", position)
                }
                context.startActivity(intent)
            }
            true
        }

        // ✅ เปลี่ยนการใช้ RxJava1 → RxJava2
        // ตัวอย่าง: ใช้ Observable.timer แทน Observable.interval
        Observable.timer(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                // ทำงานหลังจาก 1 วินาที (ตัวอย่าง)
                // ถ้าต้องการ showCircle/hideCircle ให้หา view เหล่านั้น
            }, {
                it.printStackTrace()
            })
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRemarks: TextView = itemView.findViewById(R.id.tv_remarks)
        val tvAddress: TextView = itemView.findViewById(R.id.tv_address)
        val tvType: TextView = itemView.findViewById(R.id.tv_type)
        val tvSecurity: TextView = itemView.findViewById(R.id.tv_security)
        val tvAlterId: TextView = itemView.findViewById(R.id.tv_alter_id)
        val imgCheck: ImageView = itemView.findViewById(R.id.img_check)
    }
}