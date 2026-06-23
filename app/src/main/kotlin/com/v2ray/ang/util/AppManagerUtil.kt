// แทนที่ทั้งไฟล์ด้วยเวอร์ชันนี้
package com.v2ray.ang.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.v2ray.ang.dto.AppInfo

object AppManagerUtil {

    suspend fun loadNetworkAppList(context: Context): ArrayList<AppInfo> {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val apps = ArrayList<AppInfo>()

        for (pkg in packages) {
            val applicationInfo = pkg.applicationInfo ?: continue

            val appName = applicationInfo.loadLabel(packageManager).toString()
            val appIcon = applicationInfo.loadIcon(packageManager)
            val isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) > 0

            val appInfo = AppInfo(
                appName = appName,
                packageName = pkg.packageName,
                appIcon = appIcon,
                isSystemApp = isSystemApp,
                isSelected = 0
            )
            apps.add(appInfo)
        }
        return apps
    }
}