package com.v2ray.ang.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.tbruyelle.rxpermissions2.RxPermissions // ใช้ rxpermissions2 สำหรับ AndroidX
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.QRCodeDecoder
import me.dm7.barcodescanner.zxing.ZXingScannerView

class ScannerActivity : BaseActivity(), ZXingScannerView.ResultHandler {
    companion object {
        private const val REQUEST_FILE_CHOOSER = 2
    }

    private var mScannerView: ZXingScannerView? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        mScannerView = ZXingScannerView(this)

        mScannerView?.setAutoFocus(true)
        val formats = arrayListOf(BarcodeFormat.QR_CODE)
        mScannerView?.setFormats(formats)

        setContentView(mScannerView)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        mScannerView?.setResultHandler(this)
        mScannerView?.startCamera()
    }

    override fun onPause() {
        super.onPause()
        mScannerView?.stopCamera()
    }

    override fun handleResult(rawResult: Result) {
        finished(rawResult.text)
    }

    private fun finished(text: String) {
        val intent = Intent()
        intent.putExtra("SCAN_RESULT", text)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_scanner, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.select_photo -> {
            // ใช้ RxPermissions2 เพื่อรองรับ AndroidX
            RxPermissions(this)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted) {
                        try {
                            showFileChooser()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        toast(R.string.toast_permission_denied)
                    }
                }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.title_file_chooser)),
                REQUEST_FILE_CHOOSER
            )
        } catch (ex: android.content.ActivityNotFoundException) {
            toast(R.string.toast_require_file_manager)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE_CHOOSER && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val text = QRCodeDecoder.syncDecodeQRCode(bitmap)
                        if (text != null) {
                            finished(text)
                        } else {
                            toast(R.string.toast_failure)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    toast(e.message ?: "Error")
                }
            }
        }
    }
}
