package com.v2ray.ang.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivitySubEditBinding
import com.v2ray.ang.dto.AngConfig
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.Utils

class SubEditActivity : BaseActivity() {

    private lateinit var binding: ActivitySubEditBinding

    private var delConfig: MenuItem? = null
    private var saveConfig: MenuItem? = null

    private lateinit var configs: AngConfig
    private var editIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configs = AngConfigManager.configs
        editIndex = intent.getIntExtra("position", -1)

        title = getString(R.string.title_sub_setting)

        if (editIndex >= 0) {
            bindingServer(configs.subItem[editIndex])
        } else {
            clearServer()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindingServer(subItem: AngConfig.SubItemBean): Boolean {
        binding.etRemarks.text = Utils.getEditable(subItem.remarks)
        binding.etUrl.text = Utils.getEditable(subItem.url)
        return true
    }

    private fun clearServer(): Boolean {
        binding.etRemarks.text = null
        binding.etUrl.text = null
        return true
    }

    private fun saveServer(): Boolean {
        val subItem = if (editIndex >= 0) configs.subItem[editIndex] else AngConfig.SubItemBean()

        subItem.remarks = binding.etRemarks.text.toString()
        subItem.url = binding.etUrl.text.toString()

        if (TextUtils.isEmpty(subItem.remarks)) {
            toast(R.string.sub_setting_remarks)
            return false
        }
        if (TextUtils.isEmpty(subItem.url)) {
            toast(R.string.sub_setting_url)
            return false
        }

        if (AngConfigManager.addSubItem(subItem, editIndex) == 0) {
            toast(R.string.toast_success)
            finish()
            return true
        } else {
            toast(R.string.toast_failure)
            return false
        }
    }

    private fun deleteServer(): Boolean {
        if (editIndex >= 0) {
            AlertDialog.Builder(this)
                .setMessage(R.string.del_config_comfirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (AngConfigManager.removeSubItem(editIndex) == 0) {
                        toast(R.string.toast_success)
                        finish()
                    } else {
                        toast(R.string.toast_failure)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_server, menu)
        delConfig = menu?.findItem(R.id.del_config)
        saveConfig = menu?.findItem(R.id.save_config)

        if (editIndex < 0) {
            delConfig?.isVisible = false
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.del_config -> {
            deleteServer()
            true
        }
        R.id.save_config -> {
            saveServer()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
