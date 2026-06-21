package com.v2ray.ang.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityTaskerBinding
import com.v2ray.ang.util.AngConfigManager

class TaskerActivity : BaseActivity() {
    private lateinit var binding: ActivityTaskerBinding
    private val lstData: ArrayList<String> = ArrayList()
    private val lstGuid: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ แก้ไข: ใช้ข้อความ "Default" แทน R.string.default_text ที่ไม่มีอยู่
        lstData.add("Default")
        lstGuid.add(AppConfig.TASKER_DEFAULT_GUID)

        AngConfigManager.configs.vmess.forEach {
            lstData.add(it.remarks)
            lstGuid.add(it.guid)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, lstData)
        binding.listview.adapter = adapter
        binding.listview.choiceMode = android.widget.ListView.CHOICE_MODE_SINGLE

        init()
    }

    private fun init() {
        val bundle = intent?.getBundleExtra(AppConfig.TASKER_EXTRA_BUNDLE)
        val switch = bundle?.getBoolean(AppConfig.TASKER_EXTRA_BUNDLE_SWITCH, false) ?: false
        val guid = bundle?.getString(AppConfig.TASKER_EXTRA_BUNDLE_GUID, "")

        if (!TextUtils.isEmpty(guid)) {
            binding.switchStartService.isChecked = switch
            val pos = lstGuid.indexOf(guid)
            if (pos >= 0) {
                binding.listview.setItemChecked(pos, true)
            }
        }
    }

    private fun confirmFinish() {
        val position = binding.listview.checkedItemPosition
        if (position < 0) return

        val extraBundle = Bundle().apply {
            putBoolean(AppConfig.TASKER_EXTRA_BUNDLE_SWITCH, binding.switchStartService.isChecked)
            putString(AppConfig.TASKER_EXTRA_BUNDLE_GUID, lstGuid[position])
        }

        val remarks = lstData[position]
        val blurb = if (binding.switchStartService.isChecked) "Start $remarks" else "Stop $remarks"

        val intent = Intent().apply {
            putExtra(AppConfig.TASKER_EXTRA_BUNDLE, extraBundle)
            putExtra(AppConfig.TASKER_EXTRA_STRING_BLURB, blurb)
        }
        
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_server, menu)
        menu?.findItem(R.id.del_config)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.save_config -> {
            confirmFinish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}