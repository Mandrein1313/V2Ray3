package com.v2ray.ang.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityServer2Binding
import com.v2ray.ang.dto.AngConfig
import com.v2ray.ang.extension.defaultDPreference
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.Utils
import java.lang.Exception

class Server2Activity : BaseActivity() {
    private lateinit var binding: ActivityServer2Binding

    private var delConfig: MenuItem? = null
    private var saveConfig: MenuItem? = null

    private lateinit var configs: AngConfig
    private var editIndex: Int = -1
    private var editGuid: String = ""
    private var isRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServer2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        configs = AngConfigManager.configs
        editIndex = intent.getIntExtra("position", -1)
        isRunning = intent.getBooleanExtra("isRunning", false)
        title = getString(R.string.title_server)

        if (editIndex >= 0) {
            editGuid = configs.vmess[editIndex].guid
            bindingServer(configs.vmess[editIndex])
        } else {
            clearServer()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindingServer(vmess: AngConfig.VmessBean): Boolean {
        // ใช้ชื่อ ID จาก XML ตรงๆ (et_remarks -> etRemarks, tv_content -> tvContent)
        binding.etRemarks.text = Utils.getEditable(vmess.remarks)
        val content = defaultDPreference.getPrefString(AppConfig.ANG_CONFIG + editGuid, "")
        binding.tvContent.text = Editable.Factory.getInstance().newEditable(content)
        return true
    }

    private fun clearServer(): Boolean {
        binding.etRemarks.text = null
        return true
    }

    private fun saveServer(): Boolean {
        val vmess = configs.vmess[editIndex]
        vmess.remarks = binding.etRemarks.text.toString()

        if (TextUtils.isEmpty(vmess.remarks)) {
            toast(R.string.server_lab_remarks)
            return false
        }

        try {
            // ตรวจสอบ JSON ความถูกต้อง
            Gson().fromJson(binding.tvContent.text.toString(), Any::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            toast(R.string.toast_malformed_josn)
            return false
        }

        if (AngConfigManager.addCustomServer(vmess, editIndex) == 0) {
            defaultDPreference.setPrefString(AppConfig.ANG_CONFIG + editGuid, binding.tvContent.text.toString())
            AngConfigManager.genStoreV2rayConfigIfActive(editIndex)
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
                    if (AngConfigManager.removeServer(editIndex) == 0) {
                        toast(R.string.toast_success)
                        finish()
                    } else {
                        toast(R.string.toast_failure)
                    }
                }
                .show()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_server, menu)
        delConfig = menu?.findItem(R.id.del_config)
        saveConfig = menu?.findItem(R.id.save_config)

        if (editIndex >= 0) {
            if (isRunning && editIndex == configs.index) {
                delConfig?.isVisible = false
                saveConfig?.isVisible = false
            }
        } else {
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
