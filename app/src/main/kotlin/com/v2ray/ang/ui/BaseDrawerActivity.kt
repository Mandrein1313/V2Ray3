package com.v2ray.ang.ui

import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.navigation.NavigationView
import com.v2ray.ang.R

abstract class BaseDrawerActivity : BaseActivity() {
    companion object {
        private const val TAG = "BaseDrawerActivity"
    }

    private var mToolbar: Toolbar? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var mToolbarInitialized: Boolean = false
    private var mItemToOpenWhenDrawerCloses = -1

    private val backStackChangedListener = FragmentManager.OnBackStackChangedListener { updateDrawerToggle() }

    private val drawerListener = object : DrawerLayout.DrawerListener {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            mDrawerToggle?.onDrawerSlide(drawerView, slideOffset)
        }

        override fun onDrawerOpened(drawerView: View) {
            mDrawerToggle?.onDrawerOpened(drawerView)
        }

        override fun onDrawerClosed(drawerView: View) {
            mDrawerToggle?.onDrawerClosed(drawerView)

            if (mItemToOpenWhenDrawerCloses >= 0) {
                val extras = ActivityOptions.makeCustomAnimation(
                    this@BaseDrawerActivity, R.anim.fade_in, R.anim.fade_out
                ).toBundle()
                var activityClass: Class<*>? = null
                when (mItemToOpenWhenDrawerCloses) {
                    R.id.sub_setting -> activityClass = SubSettingActivity::class.java
                    R.id.settings -> activityClass = SettingsActivity::class.java
                    R.id.logcat -> {
                        startActivity(Intent(this@BaseDrawerActivity, LogcatActivity::class.java))
                        return
                    }
                }
                if (activityClass != null) {
                    startActivity(Intent(this@BaseDrawerActivity, activityClass), extras)
                    finish()
                }
            }
        }

        override fun onDrawerStateChanged(newState: Int) {
            mDrawerToggle?.onDrawerStateChanged(newState)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Activity onCreate")
    }

    override fun onStart() {
        super.onStart()
        if (!mToolbarInitialized) {
            throw IllegalStateException("You must run super.initializeToolbar at the end of your onCreate method")
        }
    }

    override fun onResume() {
        super.onResume()
        supportFragmentManager.addOnBackStackChangedListener(backStackChangedListener)
    }

    override fun onPause() {
        super.onPause()
        supportFragmentManager.removeOnBackStackChangedListener(backStackChangedListener)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle?.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDrawerToggle?.onOptionsItemSelected(item) == true) {
            return true
        }
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (mDrawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            mDrawerLayout?.closeDrawers()
            return
        }
        val fm = supportFragmentManager
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    private fun updateDrawerToggle() {
        val toggle = mDrawerToggle ?: return
        val isRoot = supportFragmentManager.backStackEntryCount == 0
        toggle.isDrawerIndicatorEnabled = isRoot

        supportActionBar?.apply {
            setDisplayShowHomeEnabled(!isRoot)
            setDisplayHomeAsUpEnabled(!isRoot)
            setHomeButtonEnabled(!isRoot)
        }

        if (isRoot) {
            toggle.syncState()
        }
    }

    protected fun initializeToolbar() {
        mToolbar = findViewById(R.id.toolbar)
            ?: throw IllegalStateException("Layout is required to include a Toolbar with id 'toolbar'")

        mDrawerLayout = findViewById(R.id.drawer_layout)
        if (mDrawerLayout != null) {
            val navigationView = findViewById<NavigationView>(R.id.nav_view)
                ?: throw IllegalStateException("Layout requires a NavigationView with id 'nav_view'")

            mDrawerToggle = ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
            )

            mDrawerLayout?.addDrawerListener(drawerListener)

            populateDrawerItems(navigationView)
            setSupportActionBar(mToolbar)
            updateDrawerToggle()
        } else {
            setSupportActionBar(mToolbar)
        }

        mToolbarInitialized = true
    }

    private fun populateDrawerItems(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            mItemToOpenWhenDrawerCloses = menuItem.itemId
            mDrawerLayout?.closeDrawers()
            true
        }

        if (SubSettingActivity::class.java.isAssignableFrom(javaClass)) {
            navigationView.setCheckedItem(R.id.sub_setting)
        } else if (SettingsActivity::class.java.isAssignableFrom(javaClass)) {
            navigationView.setCheckedItem(R.id.settings)
        }
    }
}
