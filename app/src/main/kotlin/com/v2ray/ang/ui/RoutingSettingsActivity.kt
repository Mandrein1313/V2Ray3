package com.v2ray.ang.ui

import android.graphics.Color
import android.os.Bundle
// เปลี่ยนจาก import android.support.v4.app.Fragment เป็นตัวนี้:
import androidx.fragment.app.Fragment 
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
// ลบ kotlinx.android.synthetic ออก แล้วเพิ่ม ViewBinding:
import com.v2ray.ang.databinding.ActivityRoutingSettingsBinding

class RoutingSettingsActivity : BaseActivity() {
    
    // ประกาศ binding
    private lateinit var binding: ActivityRoutingSettingsBinding
    
    private val titles: Array<out String> by lazy {
        resources.getStringArray(R.array.routing_tag)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ใช้ ViewBinding แทน setContentView(R.layout...)
        binding = ActivityRoutingSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.routing_settings_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fragments = ArrayList<Fragment>()
        fragments.add(RoutingSettingsFragment().newInstance(AppConfig.PREF_V2RAY_ROUTING_AGENT))
        fragments.add(RoutingSettingsFragment().newInstance(AppConfig.PREF_V2RAY_ROUTING_DIRECT))
        fragments.add(RoutingSettingsFragment().newInstance(AppConfig.PREF_V2RAY_ROUTING_BLOCKED))

        val adapter = FragmentAdapter(supportFragmentManager, fragments, titles.toList())
        
        // เข้าถึง viewpager และ tablayout ผ่าน binding แทนการเรียกผ่าน id ตรงๆ
        binding.viewpager.adapter = adapter
        binding.tablayout.setTabTextColors(Color.BLACK, Color.RED)
        binding.tablayout.setupWithViewPager(binding.viewpager)
    }
}
