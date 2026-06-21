package com.v2ray.ang.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.AngConfigManager

class ScScannerActivity : BaseActivity() {
    companion object {
        private const val REQUEST_SCAN = 1
    }

    // ✅ Launcher สำหรับขอสิทธิ์กล้อง
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startActivityForResult(Intent(this, ScannerActivity::class.java), REQUEST_SCAN)
        } else {
            toast(R.string.toast_permission_denied)
            finish() // ปิด Activity หากไม่ได้รับอนุญาต
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_none)
        importQRcode(REQUEST_SCAN)
    }

    // ✅ เปลี่ยนจาก RxPermissions เป็น cameraPermissionLauncher
    fun importQRcode(requestCode: Int): Boolean {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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