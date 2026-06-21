package com.v2ray.ang.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityBypassListBinding
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.extension.defaultDPreference
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.v2RayApplication
import com.v2ray.ang.util.AppManagerUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.net.URL
import java.text.Collator
import java.util.*

class PerAppProxyActivity : BaseActivity() {
    private lateinit var binding: ActivityBypassListBinding
    private var adapter: PerAppProxyAdapter? = null
    private var appsAll: List<AppInfo>? = null

    companion object {
        const val PREF_PER_APP_PROXY_SET = "pref_per_app_proxy_set"
        const val PREF_BYPASS_APPS = "pref_bypass_apps"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBypassListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val dividerItemDecoration = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        binding.recyclerView.addItemDecoration(dividerItemDecoration)

        val blacklist = defaultDPreference.getPrefStringSet(PREF_PER_APP_PROXY_SET, null)

        AppManagerUtil.rxLoadNetworkAppList(this)
            .subscribeOn(Schedulers.io())
            .map { list ->
                if (blacklist != null) {
                    list.forEach { it.isSelected = if (blacklist.contains(it.packageName)) 1 else 0 }
                    list.sortedWith(compareByDescending { it.isSelected })
                } else {
                    val collator = Collator.getInstance()
                    list.sortedWith(Comparator { o1, o2 -> collator.compare(o1.appName, o2.appName) })
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                appsAll = it
                adapter = PerAppProxyAdapter(this, it, blacklist)
                binding.recyclerView.adapter = adapter
                binding.pbWaiting.visibility = View.GONE
            }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var dst = 0
            val threshold = resources.getDimensionPixelSize(R.dimen.bypass_list_header_height) * 3
            
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                dst += dy
                if (dst > threshold) {
                    binding.headerView.hide()
                    dst = 0
                } else if (dst < -20) {
                    binding.headerView.show()
                    dst = 0
                }
            }

            fun View.hide() {
                val target = -height.toFloat()
                if (translationY == target) return
                animate().translationY(target).setInterpolator(AccelerateInterpolator(2F)).setListener(null)
            }

            fun View.show() {
                if (translationY == 0f) return
                animate().translationY(0f).setInterpolator(DecelerateInterpolator(2F)).setListener(null)
            }
        })

        binding.switchPerAppProxy.setOnCheckedChangeListener { _, isChecked ->
            defaultDPreference.setPrefBoolean(SettingsActivity.PREF_PER_APP_PROXY, isChecked)
        }
        binding.switchPerAppProxy.isChecked = defaultDPreference.getPrefBoolean(SettingsActivity.PREF_PER_APP_PROXY, false)

        binding.switchBypassApps.setOnCheckedChangeListener { _, isChecked ->
            defaultDPreference.setPrefBoolean(PREF_BYPASS_APPS, isChecked)
        }
        binding.switchBypassApps.isChecked = defaultDPreference.getPrefBoolean(PREF_BYPASS_APPS, false)

        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                val key = v.text.toString().uppercase(Locale.getDefault())
                val filtered = appsAll?.filter { it.appName.uppercase(Locale.getDefault()).contains(key) } ?: emptyList()
                
                adapter = PerAppProxyAdapter(this, filtered, adapter?.blacklist)
                binding.recyclerView.adapter = adapter
                true
            } else false
        }
    }

    override fun onPause() {
        super.onPause()
        adapter?.let { defaultDPreference.setPrefStringSet(PREF_PER_APP_PROXY_SET, it.blacklist) }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bypass_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.select_all -> {
            adapter?.let { adp ->
                val pkgNames = adp.apps.map { it.packageName }
                if (adp.blacklist.containsAll(pkgNames)) adp.blacklist.removeAll(pkgNames)
                else adp.blacklist.addAll(pkgNames)
                adp.notifyDataSetChanged()
            }
            true
        }
        R.id.select_proxy_app -> { selectProxyApp(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun selectProxyApp() {
        toast(R.string.msg_downloading_content)
        GlobalScope.launch(Dispatchers.IO) {
            val content = try { URL(AppConfig.androidpackagenamelistUrl).readText() } catch (e: Exception) { "" }
            launch(Dispatchers.Main) { selectProxyApp(content); toast(R.string.toast_success) }
        }
    }

    private fun selectProxyApp(content: String): Boolean {
        val proxyApps = if (content.isEmpty()) Utils.readTextFromAssets(v2RayApplication, "proxy_packagename.txt") else content
        if (proxyApps.isEmpty()) return false

        adapter?.blacklist?.clear()
        adapter?.let { adp ->
            adp.apps.forEach { app ->
                val contains = proxyApps.contains(app.packageName)
                if (if (binding.switchBypassApps.isChecked) !contains else contains) {
                    adp.blacklist.add(app.packageName)
                }
            }
            adp.notifyDataSetChanged()
        }
        return true
    }
}
