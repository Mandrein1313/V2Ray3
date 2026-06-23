package com.v2ray.ang.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.v2ray.ang.dto.AppInfo
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe

object AppManagerUtil {

    fun rxLoadNetworkAppList(context: Context): Observable<List<AppInfo>> {
        return Observable.create(ObservableOnSubscribe<List<AppInfo>> { emitter: ObservableEmitter<List<AppInfo>> ->
            try {
                val packageManager = context.packageManager
                val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                val appInfoList = mutableListOf<AppInfo>()

                for (app in packages) {
                    val appInfo = AppInfo()
                    appInfo.packageName = app.packageName
                    appInfo.appName = packageManager.getApplicationLabel(app).toString()
                    appInfo.icon = app.loadIcon(packageManager)
                    appInfoList.add(appInfo)
                }

                emitter.onNext(appInfoList)
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        })
    }
}