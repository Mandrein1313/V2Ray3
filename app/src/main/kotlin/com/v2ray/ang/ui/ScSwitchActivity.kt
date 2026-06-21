package com.v2ray.ang.ui

import android.os.Bundle
import com.v2ray.ang.R
import com.v2ray.ang.service.V2RayServiceManager
import com.v2ray.ang.util.Utils

class ScSwitchActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ย้าย Activity ไปอยู่เบื้องหลังทันที
        moveTaskToBack(true)
        
        setContentView(R.layout.activity_none)

        // ตรวจสอบสถานะการทำงานของ V2Ray และสลับการทำงาน
        if (V2RayServiceManager.v2rayPoint.isRunning) {
            Utils.stopVService(this)
        } else {
            Utils.startVServiceFromToggle(this)
        }
        
        // จบ Activity ทันทีหลังจากสั่งงาน Service แล้ว
        finish()
    }
}
