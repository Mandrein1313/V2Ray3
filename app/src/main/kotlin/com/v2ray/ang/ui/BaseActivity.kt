package com.v2ray.ang.ui

import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem

abstract class BaseActivity : AppCompatActivity() {

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()           // ยังใช้ได้ชั่วคราว (หรือเปลี่ยนเป็น finish())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ถ้าอยากแก้แบบใหม่ (แนะนำสำหรับ AndroidX)
    /*
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()                  // หรือใช้ onBackPressedDispatcher
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    */
}