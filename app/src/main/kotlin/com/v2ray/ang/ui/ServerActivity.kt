package com.v2ray.ang.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityServerBinding
import com.v2ray.ang.dto.AngConfig
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.Utils

class ServerActivity : BaseActivity() {

    companion object {
        private const val REQUEST_SCAN = 1
    }

    private lateinit var binding: ActivityServerBinding

    private var delConfig: MenuItem? = null
    private var saveConfig: MenuItem? = null

    private lateinit var configs: AngConfig
    private var editIndex: Int = -1
    private var editGuid: String = ""
    private var isRunning: Boolean = false

    private val securitys: Array<out String> by lazy {
        resources.getStringArray(R.array.securitys)
    }
    private val networks: Array<out String> by lazy {
        resources.getStringArray(R.array.networks)
    }
    private val headertypes: Array<out String> by lazy {
        resources.getStringArray(R.array.headertypes)
    }
    private val streamsecuritys: Array<out String> by lazy {
        resources.getStringArray(R.array.streamsecuritys)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityServerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configs = AngConfigManager.configs
        editIndex = intent.getIntExtra("position", -1)
        isRunning = intent.getBooleanExtra("isRunning", false)
        title = getString(R.string.title_server)

        if (editIndex >= 0) {
            editGuid = configs.vmess[editIndex].guid
            bindServer(configs.vmess[editIndex])
        } else {
            clearServer()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindServer(vmess: AngConfig.VmessBean): Boolean {
        binding.etRemarks.setText(Utils.getEditable(vmess.remarks))
        binding.etAddress.setText(Utils.getEditable(vmess.address))
        binding.etPort.setText(Utils.getEditable(vmess.port.toString()))
        binding.etId.setText(Utils.getEditable(vmess.id))
        binding.etAlterId.setText(Utils.getEditable(vmess.alterId.toString()))

        val security = Utils.arrayFind(securitys, vmess.security)
        if (security >= 0) binding.spSecurity.setSelection(security)

        val network = Utils.arrayFind(networks, vmess.network)
        if (network >= 0) binding.spNetwork.setSelection(network)

        val headerType = Utils.arrayFind(headertypes, vmess.headerType)
        if (headerType >= 0) binding.spHeaderType.setSelection(headerType)

        binding.etRequestHost.setText(Utils.getEditable(vmess.requestHost))
        binding.etPath.setText(Utils.getEditable(vmess.path))

        val streamSecurity = Utils.arrayFind(streamsecuritys, vmess.streamSecurity)
        if (streamSecurity >= 0) binding.spStreamSecurity.setSelection(streamSecurity)

        return true
    }

    private fun clearServer(): Boolean {
        binding.etRemarks.text = null
        binding.etAddress.text = null
        binding.etPort.setText(Utils.getEditable("10086"))
        binding.etId.text = null
        binding.etAlterId.setText(Utils.getEditable("64"))

        binding.spSecurity.setSelection(0)
        binding.spNetwork.setSelection(0)
        binding.spHeaderType.setSelection(0)
        binding.etRequestHost.text = null
        binding.etPath.text = null
        binding.spStreamSecurity.setSelection(0)

        return true
    }

    private fun saveServer(): Boolean {
        val vmess: AngConfig.VmessBean = if (editIndex >= 0) {
            configs.vmess[editIndex]
        } else {
            AngConfig.VmessBean()
        }

        vmess.guid = editGuid
        vmess.remarks = binding.etRemarks.text.toString()
        vmess.address = binding.etAddress.text.toString()
        vmess.port = Utils.parseInt(binding.etPort.text.toString())
        vmess.id = binding.etId.text.toString()
        vmess.alterId = Utils.parseInt(binding.etAlterId.text.toString())
        vmess.security = securitys[binding.spSecurity.selectedItemPosition]
        vmess.network = networks[binding.spNetwork.selectedItemPosition]
        vmess.headerType = headertypes[binding.spHeaderType.selectedItemPosition]
        vmess.requestHost = binding.etRequestHost.text.toString()
        vmess.path = binding.etPath.text.toString()
        vmess.streamSecurity = streamsecuritys[binding.spStreamSecurity.selectedItemPosition]

        if (vmess.remarks.isEmpty()) {
            toast(R.string.server_lab_remarks)
            return false
        }
        if (vmess.address.isEmpty()) {
            toast(R.string.server_lab_address)
            return false
        }
        if (vmess.port <= 0) {
            toast(R.string.server_lab_port)
            return false
        }
        if (vmess.id.isEmpty()) {
            toast(R.string.server_lab_id)
            return false
        }
        if (vmess.alterId < 0) {
            toast(R.string.server_lab_alterid)
            return false
        }

        if (AngConfigManager.addServer(vmess, editIndex) == 0) {
            AngConfigManager.genStoreV2rayConfigIfActive(editIndex)
            toast(R.string.toast_success)
            finish()
            return true
        } else {
            toast(R.string.toast_failure)
            return false
        }
    }

    private fun deleteServer() {
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
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
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