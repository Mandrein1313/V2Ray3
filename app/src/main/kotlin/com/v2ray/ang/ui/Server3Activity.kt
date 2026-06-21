package com.v2ray.ang.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityServer3Binding
import com.v2ray.ang.dto.AngConfig
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.Utils

class Server3Activity : BaseActivity() {
    private lateinit var binding: ActivityServer3Binding

    var delConfig: MenuItem? = null
    var saveConfig: MenuItem? = null

    private lateinit var configs: AngConfig
    private var editIndex: Int = -1
    private var editGuid: String = ""
    private var isRunning: Boolean = false
    private val securitys: Array<out String> by lazy {
        resources.getStringArray(R.array.ss_securitys)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServer3Binding.inflate(layoutInflater)
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
        binding.etRemarks.text = Utils.getEditable(vmess.remarks)
        binding.etAddress.text = Utils.getEditable(vmess.address)
        binding.etPort.text = Utils.getEditable(vmess.port.toString())
        binding.etId.text = Utils.getEditable(vmess.id)
        
        val security = Utils.arrayFind(securitys, vmess.security)
        if (security >= 0) {
            binding.spSecurity.setSelection(security)
        }
        return true
    }

    private fun clearServer(): Boolean {
        binding.etRemarks.text = null
        binding.etAddress.text = null
        binding.etPort.text = Utils.getEditable("10086")
        binding.etId.text = null
        binding.spSecurity.setSelection(0)
        return true
    }

    private fun saveServer(): Boolean {
        val vmess = if (editIndex >= 0) configs.vmess[editIndex] else AngConfig.VmessBean()

        vmess.guid = editGuid
        vmess.remarks = binding.etRemarks.text.toString()
        vmess.address = binding.etAddress.text.toString()
        vmess.port = Utils.parseInt(binding.etPort.text.toString())
        vmess.id = binding.etId.text.toString()
        vmess.security = securitys[binding.spSecurity.selectedItemPosition]

        if (TextUtils.isEmpty(vmess.remarks)) {
            toast(R.string.server_lab_remarks)
            return false
        }
        if (TextUtils.isEmpty(vmess.address)) {
            toast(R.string.server_lab_address3)
            return false
        }
        if (vmess.port <= 0) {
            toast(R.string.server_lab_port3)
            return false
        }
        if (TextUtils.isEmpty(vmess.id)) {
            toast(R.string.server_lab_id3)
            return false
        }

        if (AngConfigManager.addShadowsocksServer(vmess, editIndex) == 0) {
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

        if (editIndex >= 0 && isRunning && editIndex == configs.index) {
            delConfig?.isVisible = false
            saveConfig?.isVisible = false
        } else if (editIndex < 0) {
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
