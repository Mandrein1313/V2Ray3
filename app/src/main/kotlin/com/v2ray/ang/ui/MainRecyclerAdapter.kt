package com.v2ray.ang.ui

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ItemRecyclerFooterBinding
import com.v2ray.ang.databinding.ItemRecyclerMainBinding
import com.v2ray.ang.databinding.ItemQrcodeBinding
import com.v2ray.ang.dto.AngConfig
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.helper.ItemTouchHelperAdapter
import com.v2ray.ang.helper.ItemTouchHelperViewHolder
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.Utils
import com.v2ray.ang.util.V2rayConfigUtil
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class MainRecyclerAdapter(val activity: MainActivity) : RecyclerView.Adapter<MainRecyclerAdapter.BaseViewHolder>(), ItemTouchHelperAdapter {
    companion object {
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_FOOTER = 2
    }

    private val mActivity = activity
    private lateinit var configs: AngConfig
    private val shareMethod: Array<out String> by lazy {
        mActivity.resources.getStringArray(R.array.share_method)
    }

    var changeable: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    init {
        updateConfigList()
    }

    override fun getItemCount() = configs.vmess.count() + 1

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (holder is MainViewHolder) {
            val item = configs.vmess[position]
            val configType = EConfigType.fromInt(item.configType)
            val b = holder.binding
            
            b.tvName.text = item.remarks
            b.btnRadio.isChecked = (position == configs.index)
            b.root.setBackgroundColor(Color.TRANSPARENT)
            b.tvTestResult.text = item.testResult
            b.tvSubscription.text = configs.subItem.find { it.id == item.subid }?.remarks ?: ""

            var shareOptions = shareMethod.toList()
            if (configType == EConfigType.CUSTOM) {
                b.tvType.text = mActivity.getString(R.string.server_customize_config)
                val serverOutbound = V2rayConfigUtil.getCustomConfigServerOutbound(mActivity.applicationContext, item.guid)
                b.tvStatistics.text = serverOutbound?.let { "${it.getServerAddress()} : ${it.getServerPort()}" } ?: ""
                shareOptions = shareOptions.takeLast(1)
            } else {
                b.tvType.text = configType?.name?.lowercase()
                b.tvStatistics.text = "${item.address} : ${item.port}"
            }

            b.layoutShare.setOnClickListener {
                AlertDialog.Builder(mActivity).setItems(shareOptions.toTypedArray()) { _, i ->
                    try {
                        when (i) {
                            0 -> if (configType == EConfigType.CUSTOM) shareFullContent(position) else {
                                val bindingQrcode = ItemQrcodeBinding.inflate(mActivity.layoutInflater)
                                bindingQrcode.ivQcode.setImageBitmap(AngConfigManager.share2QRCode(position))
                                AlertDialog.Builder(mActivity).setView(bindingQrcode.root).show()
                            }
                            1 -> if (AngConfigManager.share2Clipboard(position) == 0) mActivity.toast(R.string.toast_success) else mActivity.toast(R.string.toast_failure)
                            2 -> shareFullContent(position)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }.show()
            }

            b.layoutEdit.setOnClickListener {
                val intent = Intent().putExtra("position", position).putExtra("isRunning", !changeable)
                val cls = when (configType) {
                    EConfigType.VMESS -> ServerActivity::class.java
                    EConfigType.CUSTOM -> Server2Activity::class.java
                    EConfigType.SHADOWSOCKS -> Server3Activity::class.java
                    EConfigType.SOCKS -> Server4Activity::class.java
                    else -> null
                }
                if (cls != null) mActivity.startActivity(intent.setClass(mActivity, cls))
            }

            b.layoutRemove.setOnClickListener {
                if (configs.index != position && AngConfigManager.removeServer(position) == 0) {
                    notifyItemRemoved(position)
                    updateSelectedItem(position)
                }
            }

            b.infoContainer.setOnClickListener {
                if (changeable) {
                    AngConfigManager.setActiveServer(position)
                } else {
                    mActivity.showCircle()
                    Utils.stopVService(mActivity)
                    Observable.timer(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            mActivity.showCircle()
                            if (!Utils.startVService(mActivity, position)) mActivity.hideCircle()
                        }
                }
                notifyDataSetChanged()
            }
        } else if (holder is FooterViewHolder) {
            holder.binding.layoutEdit.setOnClickListener { Utils.openUri(mActivity, AppConfig.promotionUrl) }
        }
    }

    private fun shareFullContent(position: Int) {
        if (AngConfigManager.shareFullContent2Clipboard(position) == 0) mActivity.toast(R.string.toast_success)
        else mActivity.toast(R.string.toast_failure)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_ITEM) 
            MainViewHolder(ItemRecyclerMainBinding.inflate(inflater, parent, false))
        else 
            FooterViewHolder(ItemRecyclerFooterBinding.inflate(inflater, parent, false))
    }

    fun updateConfigList() { configs = AngConfigManager.configs; notifyDataSetChanged() }
    fun updateSelectedItem(pos: Int) { notifyItemRangeChanged(pos, itemCount - pos) }

    override fun getItemViewType(position: Int) = if (position == configs.vmess.count()) VIEW_TYPE_FOOTER else VIEW_TYPE_ITEM

    open class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class MainViewHolder(val binding: ItemRecyclerMainBinding) : BaseViewHolder(binding.root), ItemTouchHelperViewHolder {
        override fun onItemSelected() { itemView.setBackgroundColor(Color.LTGRAY) }
        override fun onItemClear() { itemView.setBackgroundColor(Color.TRANSPARENT) }
    }

    class FooterViewHolder(val binding: ItemRecyclerFooterBinding) : BaseViewHolder(binding.root), ItemTouchHelperViewHolder {
        override fun onItemSelected() { itemView.setBackgroundColor(Color.LTGRAY) }
        override fun onItemClear() { itemView.setBackgroundColor(Color.TRANSPARENT) }
    }

    override fun onItemDismiss(position: Int) {
        if (configs.index != position && AngConfigManager.removeServer(position) == 0) notifyItemRemoved(position)
        updateSelectedItem(position)
    }

    override fun onItemMove(from: Int, to: Int): Boolean {
        AngConfigManager.swapServer(from, to)
        notifyItemMoved(from, to)
        updateSelectedItem(minOf(from, to))
        return true
    }

    override fun onItemMoveCompleted() { AngConfigManager.storeConfigFile() }
}
