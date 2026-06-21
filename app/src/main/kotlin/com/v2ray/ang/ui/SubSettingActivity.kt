package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivitySubSettingBinding

class SubSettingActivity : BaseActivity() {

    private lateinit var binding: ActivitySubSettingBinding
    private val adapter by lazy { SubSettingRecyclerAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.title_sub_setting)

        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@SubSettingActivity)
            adapter = this@SubSettingActivity.adapter
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        adapter.updateConfigList()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_sub_setting, menu)
        menu?.findItem(R.id.del_config)?.isVisible = false
        menu?.findItem(R.id.save_config)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_config -> {
            startActivity(Intent(this, SubEditActivity::class.java)
                .putExtra("position", -1)
            )
            // หมายเหตุ: ไม่จำเป็นต้องเรียก updateConfigList() ที่นี่ เพราะ onResume จะจัดการให้เมื่อกลับมาหน้าเดิม
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
