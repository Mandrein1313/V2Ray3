package com.v2ray.ang.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ItemRecyclerBypassListBinding
import com.v2ray.ang.dto.AppInfo

class PerAppProxyAdapter(
    val activity: BaseActivity,
    val apps: List<AppInfo>,
    blacklist: MutableSet<String>?
) : RecyclerView.Adapter<PerAppProxyAdapter.BaseViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    // เก็บรายการ blacklist ไว้ใน HashSet เพื่อให้ค้นหาได้รวดเร็ว
    val blacklist = if (blacklist == null) HashSet<String>() else HashSet<String>(blacklist)

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (holder is AppViewHolder) {
            val appInfo = apps[position - 1]
            holder.bind(appInfo)
        }
    }

    override fun getItemCount() = apps.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = View(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                parent.context.resources.getDimensionPixelSize(R.dimen.bypass_list_header_height) * 3
            )
            BaseViewHolder(view)
        } else {
            // ใช้ ViewBinding เชื่อมต่อกับ layout
            AppViewHolder(ItemRecyclerBypassListBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemViewType(position: Int) = if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM

    open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class AppViewHolder(val binding: ItemRecyclerBypassListBinding) : BaseViewHolder(binding.root), View.OnClickListener {
        private lateinit var appInfo: AppInfo
        
        // เช็คว่าแอปอยู่ใน blacklist หรือไม่
        private val inBlacklist: Boolean get() = blacklist.contains(appInfo.packageName)

        fun bind(appInfo: AppInfo) {
            this.appInfo = appInfo

            // ใช้ชื่อตัวแปรที่ ViewBinding แปลงจาก XML ให้เรียบร้อยแล้ว (CamelCase)
            binding.icon.setImageDrawable(appInfo.appIcon)
            binding.checkBox.isChecked = inBlacklist
            binding.packageName.text = appInfo.packageName 

            if (appInfo.isSystemApp) {
                binding.name.text = String.format("** %1s", appInfo.appName)
                binding.name.setTextColor(Color.RED)
            } else {
                binding.name.text = appInfo.appName
                binding.name.setTextColor(Color.DKGRAY)
            }

            binding.root.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            // ป้องกันความผิดพลาดหากข้อมูลยังไม่ถูก bind
            if (!::appInfo.isInitialized) return

            if (inBlacklist) {
                blacklist.remove(appInfo.packageName)
                binding.checkBox.isChecked = false
            } else {
                blacklist.add(appInfo.packageName)
                binding.checkBox.isChecked = true
            }
        }
    }
}
