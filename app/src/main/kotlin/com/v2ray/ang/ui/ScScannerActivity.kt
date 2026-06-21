package com.v2ray.ang.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import com.tbruyelle.rxpermissions2.RxPermissions // ใช้ rxpermissions2 สำหรับ AndroidX
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.AngConfigManager

class ScScannerActivity : BaseActivity() {
    companion object {
        private const val REQUEST_SCAN = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ตรวจสอบให้แน่ใจว่า activity_none มีอยู่จริงใน layout
        setContentView(R.layout.activity_none)
        importQRcode(REQUEST_SCAN)
    }

    fun importQRcode(requestCode: Int): Boolean {
        // ใช้ RxPermissions ที่รองรับ AndroidX
        RxPermissions(this)
            .request(Manifest.permission.CAMERA)
            .subscribe { granted ->
                if (granted) {
                    startActivityForResult(Intent(this, ScannerActivity::class.java), requestCode)
                } else {
                    toast(R.string.toast_permission_denied)
                    finish() // ปิด Activity หากไม่ได้รับอนุญาต
                }
            }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SCAN -> {
                if (resultCode == RESULT_OK) {
                    val scanResult = data?.getStringExtra("SCAN_RESULT")
                    val count = AngConfigManager.importBatchConfig(scanResult, "")
                    
                    if (count > 0) {
                        toast(R.string.toast_success)
                    } else {
                        toast(R.string.toast_failure)
                    }
                    
                    // นำทางกลับไปที่หน้าหลัก
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                finish()
            }
        }
    }
}
