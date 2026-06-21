package com.v2ray.ang.util

import android.graphics.Bitmap
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.ANG_CONFIG
import com.v2ray.ang.AppConfig.PREF_CURR_CONFIG
import com.v2ray.ang.AppConfig.PREF_CURR_CONFIG_GUID
import com.v2ray.ang.AppConfig.PREF_CURR_CONFIG_NAME
import com.v2ray.ang.AppConfig.SOCKS_PROTOCOL
import com.v2ray.ang.AppConfig.SS_PROTOCOL
import com.v2ray.ang.AppConfig.VMESS_PROTOCOL
import com.v2ray.ang.R
import com.v2ray.ang.dto.AngConfig
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.dto.VmessQRCode
import java.net.URI
import java.net.URLDecoder
import java.util.*

object AngConfigManager {
    private lateinit var app: AngApplication
    private lateinit var angConfig: AngConfig
    val configs: AngConfig get() = angConfig

    fun inject(app: AngApplication) {
        this.app = app
        if (app.firstRun) {
            // TODO: handle first run
        }
        loadConfig()
    }

    /**
     * loading config
     */
    fun loadConfig() {
        try {
            val context = app.defaultDPreference.getPrefString(ANG_CONFIG, "")
            if (!TextUtils.isEmpty(context)) {
                angConfig = Gson().fromJson(context, AngConfig::class.java)
            } else {
                angConfig = AngConfig(0, vmess = arrayListOf(), subItem = arrayListOf())
                angConfig.index = -1
            }

            for (i in angConfig.vmess.indices) {
                upgradeServerVersion(angConfig.vmess[i])
            }

            if (configs.subItem == null) {
                configs.subItem = arrayListOf()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * add or edit server
     */
    fun addServer(vmess: AngConfig.VmessBean, index: Int): Int {
        try {
            vmess.configVersion = 2
            vmess.configType = EConfigType.VMESS.value

            if (index >= 0) {
                //edit
                angConfig.vmess[index] = vmess
            } else {
                //add
                vmess.guid = Utils.getUuid()
                angConfig.vmess.add(vmess)
                if (angConfig.vmess.size == 1) {
                    angConfig.index = 0
                }
            }

            storeConfigFile()
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return 0
    }

    /**
     * 移除服务器
     */
    fun removeServer(index: Int): Int {
        try {
            if (index < 0 || index > angConfig.vmess.size - 1) {
                return -1
            }

            //删除
            angConfig.vmess.removeAt(index)

            //移除的是活动的
            adjustIndexForRemovalAt(index)

            storeConfigFile()
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return 0
    }

    private fun adjustIndexForRemovalAt(index: Int) {
        when {
            angConfig.index == index -> {
                angConfig.index = if (angConfig.vmess.isNotEmpty()) 0 else -1
            }
            index < angConfig.index -> angConfig.index--
        }
    }

    fun swapServer(fromPosition: Int, toPosition: Int): Int {
        try {
            Collections.swap(angConfig.vmess, fromPosition, toPosition)

            val index = angConfig.index
            when (index) {
                fromPosition -> angConfig.index = toPosition
                toPosition -> angConfig.index = fromPosition
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return 0
    }

    /**
     * set active server
     */
    fun setActiveServer(index: Int): Int {
        try {
            if (index < 0 || index > angConfig.vmess.size - 1) {
                app.curIndex = -1
                return -1
            }
            angConfig.index = index
            app.curIndex = index
            storeConfigFile()
            if (!genStoreV2rayConfig()) {
                Log.d(AppConfig.ANG_PACKAGE, "set active index $index but generate full configuration failed!")
                return -1
            }
        } catch (e: Exception) {
            e.printStackTrace()
            app.curIndex = -1
            return -1
        }
        return 0
    }

    /**
     * store config to file
     */
    fun storeConfigFile() {
        try {
            val conf = Gson().toJson(angConfig)
            app.defaultDPreference.setPrefString(ANG_CONFIG, conf)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun genStoreV2rayConfigIfActive(index: Int) {
        if (index == configs.index) {
            if (!genStoreV2rayConfig()) {
                Log.d(AppConfig.ANG_PACKAGE, "update config $index but generate full configuration failed!")
            }
        }
    }

    /**
     * gen and store v2ray config file
     */
    fun genStoreV2rayConfig(): Boolean {
        return try {
            angConfig.vmess.getOrNull(angConfig.index)?.let {
                val result = V2rayConfigUtil.getV2rayConfig(app, it)
                if (result.status) {
                    app.defaultDPreference.setPrefString(PREF_CURR_CONFIG, result.content)
                    app.defaultDPreference.setPrefString(PREF_CURR_CONFIG_GUID, currConfigGuid())
                    app.defaultDPreference.setPrefString(PREF_CURR_CONFIG_NAME, currConfigName())
                    return@let true
                }
                return@let false
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun currGeneratedV2rayConfig(): String {
        return app.defaultDPreference.getPrefString(AppConfig.PREF_CURR_CONFIG, "")
    }

    fun currConfigType(): EConfigType? {
        return if (angConfig.index < 0 || angConfig.vmess.isEmpty() || angConfig.index > angConfig.vmess.size - 1) {
            null
        } else {
            EConfigType.fromInt(angConfig.vmess[angConfig.index].configType)
        }
    }

    fun currConfigName(): String {
        return if (angConfig.index < 0 || angConfig.vmess.isEmpty() || angConfig.index > angConfig.vmess.size - 1) {
            ""
        } else {
            angConfig.vmess[angConfig.index].remarks
        }
    }

    fun currConfigGuid(): String {
        return if (angConfig.index < 0 || angConfig.vmess.isEmpty() || angConfig.index > angConfig.vmess.size - 1) {
            ""
        } else {
            angConfig.vmess[angConfig.index].guid
        }
    }

    /**
     * import config form qrcode or...
     */
    fun importConfig(server: String?, subid: String, removedSelectedServer: AngConfig.VmessBean?): Int {
        try {
            if (server.isNullOrEmpty()) {
                return R.string.toast_none_data
            }

            var vmess = AngConfig.VmessBean()

            when {
                server.startsWith(VMESS_PROTOCOL) -> {
                    val indexSplit = server.indexOf("?")
                    val newVmess = tryParseNewVmess(server)
                    if (newVmess != null) {
                        vmess = newVmess
                        vmess.subid = subid
                    } else if (indexSplit > 0) {
                        vmess = resolveVmess4Kitsunebi(server)
                    } else {
                        var result = server.replace(VMESS_PROTOCOL, "")
                        result = Utils.decode(result)
                        if (TextUtils.isEmpty(result)) {
                            return R.string.toast_decoding_failed
                        }
                        val vmessQRCode = Gson().fromJson(result, VmessQRCode::class.java)
                        if (TextUtils.isEmpty(vmessQRCode.add)
                            || TextUtils.isEmpty(vmessQRCode.port)
                            || TextUtils.isEmpty(vmessQRCode.id)
                            || TextUtils.isEmpty(vmessQRCode.aid)
                            || TextUtils.isEmpty(vmessQRCode.net)
                        ) {
                            return R.string.toast_incorrect_protocol
                        }

                        vmess.apply {
                            configType = EConfigType.VMESS.value
                            security = "auto"
                            network = "tcp"
                            headerType = "none"
                            configVersion = Utils.parseInt(vmessQRCode.v)
                            remarks = vmessQRCode.ps
                            address = vmessQRCode.add
                            port = Utils.parseInt(vmessQRCode.port)
                            id = vmessQRCode.id
                            alterId = Utils.parseInt(vmessQRCode.aid)
                            network = vmessQRCode.net
                            headerType = vmessQRCode.type
                            requestHost = vmessQRCode.host
                            path = vmessQRCode.path
                            streamSecurity = vmessQRCode.tls
                            this.subid = subid
                        }
                    }
                    upgradeServerVersion(vmess)
                    addServer(vmess, -1)
                }

                server.startsWith(SS_PROTOCOL) -> {
                    var result = server.replace(SS_PROTOCOL, "")
                    val indexSplit = result.indexOf("#")
                    if (indexSplit > 0) {
                        try {
                            vmess.remarks = Utils.urlDecode(result.substring(indexSplit + 1))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        result = result.substring(0, indexSplit)
                    }

                    val indexS = result.indexOf("@")
                    result = if (indexS > 0) {
                        Utils.decode(result.substring(0, indexS)) + result.substring(indexS)
                    } else {
                        Utils.decode(result)
                    }

                    val legacyPattern = "^(.+?):(.*)@(.+?):(\\d+?)/?$".toRegex()
                    val match = legacyPattern.matchEntire(result)
                    if (match == null) {
                        return R.string.toast_incorrect_protocol
                    }
                    vmess.apply {
                        security = match.groupValues[1].lowercase()
                        id = match.groupValues[2]
                        address = match.groupValues[3]
                        if (address.firstOrNull() == '[' && address.lastOrNull() == ']') {
                            address = address.substring(1, address.length - 1)
                        }
                        port = match.groupValues[4].toInt()
                        this.subid = subid
                    }
                    addShadowsocksServer(vmess, -1)
                }

                server.startsWith(SOCKS_PROTOCOL) -> {
                    var result = server.replace(SOCKS_PROTOCOL, "")
                    val indexSplit = result.indexOf("#")
                    if (indexSplit > 0) {
                        try {
                            vmess.remarks = Utils.urlDecode(result.substring(indexSplit + 1))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        result = result.substring(0, indexSplit)
                    }

                    val indexS = result.indexOf(":")
                    if (indexS < 0) {
                        result = Utils.decode(result)
                    }

                    val legacyPattern = "^(.+?):(\\d+?)$".toRegex()
                    val match = legacyPattern.matchEntire(result)
                    if (match == null) {
                        return R.string.toast_incorrect_protocol
                    }
                    vmess.apply {
                        address = match.groupValues[1]
                        if (address.firstOrNull() == '[' && address.lastOrNull() == ']') {
                            address = address.substring(1, address.length - 1)
                        }
                        port = match.groupValues[2].toInt()
                        this.subid = subid
                    }
                    addSocksServer(vmess, -1)
                }

                else -> return R.string.toast_incorrect_protocol
            }

            if (removedSelectedServer != null &&
                vmess.subid == removedSelectedServer.subid &&
                vmess.address == removedSelectedServer.address &&
                vmess.port == removedSelectedServer.port
            ) {
                setActiveServer(configs.vmess.size - 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return 0
    }

    fun tryParseNewVmess(uri: String): AngConfig.VmessBean? {
        return runCatching {
            val uri = URI(uri)
            require(uri.scheme == "vmess") { "Invalid scheme" }
            
            val (_, protocol, tlsStr, uuid, alterId) =
                Regex("(tcp|http|ws|kcp|quic)(\\+tls)?:([0-9a-z]{8}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{12})-([0-9]+)")
                    .matchEntire(uri.userInfo)?.groupValues
                    ?: error("parse user info fail.")
            
            val tls = tlsStr.isNotBlank()
            val queryParam = uri.rawQuery.split("&")
                .map { it.split("=").let { (k, v) -> k to URLDecoder.decode(v, "utf-8") } }
                .toMap()
            
            AngConfig.VmessBean().apply {
                address = uri.host
                port = uri.port
                id = uuid
                alterId = alterId.toInt()
                streamSecurity = if (tls) "tls" else ""
                remarks = uri.fragment
                security = "auto"

                when (protocol) {
                    "tcp" -> {
                        network = "tcp"
                        headerType = queryParam["type"] ?: "none"
                        requestHost = queryParam["host"] ?: ""
                    }
                    "http" -> {
                        network = "h2"
                        path = queryParam["path"]?.takeIf { it.trim() != "/" } ?: ""
                        requestHost = queryParam["host"]?.split("|")?.getOrNull(0) ?: ""
                    }
                    "ws" -> {
                        network = "ws"
                        path = queryParam["path"]?.takeIf { it.trim() != "/" } ?: ""
                        requestHost = queryParam["host"]?.split("|")?.getOrNull(0) ?: ""
                    }
                    "kcp" -> {
                        network = "kcp"
                        headerType = queryParam["type"] ?: "none"
                        path = queryParam["seed"] ?: ""
                    }
                    "quic" -> {
                        network = "quic"
                        requestHost = queryParam["security"] ?: "none"
                        headerType = queryParam["type"] ?: "none"
                        path = queryParam["key"] ?: ""
                    }
                }
            }
        }.getOrNull()
    }

    private fun resolveVmess4Kitsunebi(server: String): AngConfig.VmessBean {
        val vmess = AngConfig.VmessBean()
        var result = server.replace(VMESS_PROTOCOL, "")
        val indexSplit = result.indexOf("?")
        if (indexSplit > 0) {
            result = result.substring(0, indexSplit)
        }
        result = Utils.decode(result)

        val arr1 = result.split('@')
        if (arr1.size != 2) return vmess
        
        val arr21 = arr1[0].split(':')
        val arr22 = arr1[1].split(':')
        if (arr21.size != 2 || arr22.size != 2) return vmess

        vmess.apply {
            address = arr22[0]
            port = Utils.parseInt(arr22[1])
            security = "chacha20-poly1305"
            id = arr21[1]
            network = "tcp"
            headerType = "none"
            remarks = "Alien"
            alterId = 0
        }
        return vmess
    }

    /**
     * share config
     */
    fun shareConfig(index: Int): String {
        return try {
            if (index < 0 || index > angConfig.vmess.size - 1) {
                return ""
            }

            val vmess = angConfig.vmess[index]
            when (angConfig.vmess[index].configType) {
                EConfigType.VMESS.value -> {
                    val vmessQRCode = VmessQRCode().apply {
                        v = vmess.configVersion.toString()
                        ps = vmess.remarks
                        add = vmess.address
                        port = vmess.port.toString()
                        id = vmess.id
                        aid = vmess.alterId.toString()
                        net = vmess.network
                        type = vmess.headerType
                        host = vmess.requestHost
                        path = vmess.path
                        tls = vmess.streamSecurity
                    }
                    VMESS_PROTOCOL + Utils.encode(Gson().toJson(vmessQRCode))
                }
                EConfigType.SHADOWSOCKS.value -> {
                    val remark = "#" + Utils.urlEncode(vmess.remarks)
                    val url = String.format("%s:%s@%s:%s",
                        vmess.security,
                        vmess.id,
                        vmess.address,
                        vmess.port)
                    SS_PROTOCOL + Utils.encode(url) + remark
                }
                EConfigType.SOCKS.value -> {
                    val remark = "#" + Utils.urlEncode(vmess.remarks)
                    val url = String.format("%s:%s",
                        vmess.address,
                        vmess.port)
                    SOCKS_PROTOCOL + Utils.encode(url) + remark
                }
                else -> ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * share2Clipboard
     */
    fun share2Clipboard(index: Int): Int {
        return try {
            val conf = shareConfig(index)
            if (TextUtils.isEmpty(conf)) {
                return -1
            }
            Utils.setClipboard(app.applicationContext, conf)
            0
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * share2Clipboard
     */
    fun shareAll2Clipboard(): Int {
        return try {
            val sb = StringBuilder()
            for (k in 0 until angConfig.vmess.size) {
                val url = shareConfig(k)
                if (TextUtils.isEmpty(url)) continue
                sb.append(url).appendln()
            }
            if (sb.isNotEmpty()) {
                Utils.setClipboard(app.applicationContext, sb.toString())
            }
            0
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * share2QRCode
     */
    fun share2QRCode(index: Int): Bitmap? {
        return try {
            val conf = shareConfig(index)
            if (TextUtils.isEmpty(conf)) {
                return null
            }
            Utils.createQRCode(conf)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * shareFullContent2Clipboard
     */
    fun shareFullContent2Clipboard(index: Int): Int {
        return try {
            val result = V2rayConfigUtil.getV2rayConfig(app, angConfig.vmess[index])
            if (result.status) {
                Utils.setClipboard(app.applicationContext, result.content)
                0
            } else {
                -1
            }
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * import customize config
     */
    fun importCustomizeConfig(server: String?): Int {
        return try {
            if (server.isNullOrEmpty()) {
                return R.string.toast_none_data
            }

            val guid = System.currentTimeMillis().toString()
            app.defaultDPreference.setPrefString(ANG_CONFIG + guid, server)

            val vmess = AngConfig.VmessBean().apply {
                configVersion = 2
                configType = EConfigType.CUSTOM.value
                this.guid = guid
                remarks = guid
                security = ""
                network = ""
                headerType = ""
                address = ""
                port = 0
                id = ""
                alterId = 0
                requestHost = ""
                streamSecurity = ""
            }

            angConfig.vmess.add(vmess)
            if (angConfig.vmess.size == 1) {
                angConfig.index = 0
            }
            storeConfigFile()
            0
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * getIndexViaGuid
     */
    fun getIndexViaGuid(guid: String): Int {
        return try {
            if (TextUtils.isEmpty(guid)) return -1
            angConfig.vmess.indexOfFirst { it.guid == guid }
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * upgrade
     */
    fun upgradeServerVersion(vmess: AngConfig.VmessBean): Int {
        return try {
            if (vmess.configVersion == 2) return 0

            when (vmess.network) {
                "ws", "h2" -> {
                    val lstParameter = vmess.requestHost.split(";")
                    vmess.path = lstParameter.getOrElse(0) { "" }.trim()
                    vmess.requestHost = lstParameter.getOrElse(1) { "" }.trim()
                }
                "kcp" -> { /* nothing to upgrade */ }
                else -> { /* nothing to upgrade */ }
            }
            vmess.configVersion = 2
            0
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private fun addGenericServer(vmess: AngConfig.VmessBean, index: Int, configType: EConfigType): Int {
        return try {
            vmess.configVersion = 2
            vmess.configType = configType.value

            if (index >= 0) {
                angConfig.vmess[index] = vmess
            } else {
                vmess.guid = System.currentTimeMillis().toString()
                angConfig.vmess.add(vmess)
                if (angConfig.vmess.size == 1) {
                    angConfig.index = 0
                }
            }
            storeConfigFile()
            0
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun addCustomServer(vmess: AngConfig.VmessBean, index: Int): Int {
        return addGenericServer(vmess, index, EConfigType.CUSTOM)
    }

    fun addShadowsocksServer(vmess: AngConfig.VmessBean, index: Int): Int {
        return addGenericServer(vmess, index, EConfigType.SHADOWSOCKS)
    }

    fun addSocksServer(vmess: AngConfig.VmessBean, index: Int): Int {
        return addGenericServer(vmess, index, EConfigType.SOCKS)
    }

    fun importBatchConfig(servers: String?, subid: String): Int {
        return try {
            if (servers == null) return 0
            
            val removedSelectedServer = if (!TextUtils.isEmpty(subid)) {
                configs.vmess.getOrNull(configs.index)?.takeIf { it.subid == subid }
            } else null
            
            removeServerViaSubid(subid)

            servers.lines()
                .map { importConfig(it, subid, removedSelectedServer) }
                .count { it == 0 }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun saveSubItem(subItem: ArrayList<AngConfig.SubItemBean>): Int {
        return try {
            if (subItem.isEmpty()) return -1
            subItem.forEach { 
                if (TextUtils.isEmpty(it.id)) {
                    it.id = Utils.getUuid()
                }
            }
            angConfig.subItem = subItem
            storeConfigFile()
            0
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun removeServerViaSubid(subid: String): Int {
        if (TextUtils.isEmpty(subid) || configs.vmess.isEmpty()) {
            return -1
        }

        for (k in configs.vmess.size - 1 downTo 0) {
            if (configs.vmess[k].subid == subid) {
                angConfig.vmess.removeAt(k)
                adjustIndexForRemovalAt(k)
            }
        }

        storeConfigFile()
        return 0
    }

    fun addSubItem(subItem: AngConfig.SubItemBean, index: Int): Int {
        return try {
            if (index >= 0) {
                angConfig.subItem[index] = subItem
            } else {
                angConfig.subItem.add(subItem)
            }
            saveSubItem(angConfig.subItem)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     *
     */
    fun removeSubItem(index: Int): Int {
        return try {
            if (index < 0 || index > angConfig.subItem.size - 1) {
                return -1
            }
            angConfig.subItem.removeAt(index)
            storeConfigFile()
            0
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}
