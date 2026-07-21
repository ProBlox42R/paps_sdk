package com.paps.updatesdk

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Drop this in any app distributed through Paps App Store to enforce
 * updates: call [checkOnLaunch] from your main activity's onCreate/onResume,
 * and if Paps says a newer version is out, it shows a dialog pointing
 * people at the update - blocking further use of the app unless
 * [allowDismiss] is set to true.
 *
 * This deliberately does nothing if Paps can't be reached (no internet,
 * server down, etc.) - a broken network check should never be the reason
 * someone can't open an app they already have installed.
 *
 * Usage:
 * ```
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     PapsUpdateChecker.checkOnLaunch(this)
 * }
 * ```
 */
object PapsUpdateChecker {

    private const val TAG = "PapsUpdateChecker"
    const val DEFAULT_BASE_URL = "https://paps.probloxworld.com/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    /**
     * @param activity The activity to show the update dialog over. Kept as
     *   a weak-ish reference only for the duration of the check - fine to
     *   call from onCreate/onResume.
     * @param serverBaseUrl Defaults to the main Paps server; override only
     *   if you're pointing at a different Paps instance.
     * @param allowDismiss If false (the default), the dialog can't be
     *   dismissed or backed out of - the person must tap Update. If true,
     *   a "Later" button is shown instead.
     */
    @JvmStatic
    @JvmOverloads
    fun checkOnLaunch(
        activity: Activity,
        serverBaseUrl: String = DEFAULT_BASE_URL,
        allowDismiss: Boolean = false
    ) {
        val packageName = activity.packageName
        val versionCode = try {
            val info = activity.packageManager.getPackageInfo(packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                info.versionCode
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read own version, skipping update check", e)
            return
        }

        val base = serverBaseUrl.trimEnd('/')
        val url = "$base/api/version-check/$packageName?versionCode=$versionCode"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Paps unreachable - don't punish the user for that.
                Log.w(TAG, "Update check failed (will just skip it): ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) return
                    try {
                        val body = it.body?.string() ?: return
                        val json = JSONObject(body)
                        if (!json.optBoolean("found", false)) return
                        if (!json.optBoolean("updateRequired", false)) return

                        val latestVersionName = json.optString("latestVersionName", "")
                        val updateUrl = json.optString("updateUrl", null)
                            ?: "$base/go/$packageName"

                        if (activity.isFinishing || activity.isDestroyed) return

                        activity.runOnUiThread {
                            showUpdateDialog(activity, latestVersionName, updateUrl, allowDismiss)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Couldn't parse update-check response, skipping", e)
                    }
                }
            }
        })
    }

    private fun showUpdateDialog(
        activity: Activity,
        latestVersionName: String,
        updateUrl: String,
        allowDismiss: Boolean
    ) {
        val message = if (latestVersionName.isNotBlank()) {
            "A newer version ($latestVersionName) of this app is available on Paps App Store."
        } else {
            "A newer version of this app is available on Paps App Store."
        }
        val fullMessage = if (allowDismiss) {
            message
        } else {
            "$message You'll need to update to keep using this app."
        }

        val builder = AlertDialog.Builder(activity)
            .setTitle("Update required")
            .setMessage(fullMessage)
            .setCancelable(allowDismiss)
            .setPositiveButton("Update") { _, _ ->
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't open update link", e)
                }
                if (!allowDismiss) activity.finish()
            }

        if (allowDismiss) {
            builder.setNegativeButton("Later", null)
        }

        val dialog = builder.create()
        dialog.setCancelable(allowDismiss)
        dialog.setCanceledOnTouchOutside(allowDismiss)
        dialog.show()
    }
}
