package com.example.checklist

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Downloads a release APK inside the app, checks it is really this app, and hands it to Android's installer. */
object Installer {
    /** Returns null when the system install prompt has been shown, otherwise a message for the user. */
    suspend fun downloadAndInstall(ctx: Context, info: UpdateInfo, onProgress: (Float) -> Unit): String? {
        if (info.apkUrl.isBlank()) return "This release has no APK attached."
        if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return "Turn on \"Allow from this source\" for Checklist, press back, then tap Install again. You only do this once."
        }
        return try {
            val apk = withContext(Dispatchers.IO) { download(ctx, info.apkUrl, onProgress) }
            val problem = verify(ctx, apk)
            if (problem != null) problem else { withContext(Dispatchers.IO) { commit(ctx, apk) }; null }
        } catch (e: IOException) {
            "Download failed. Check your connection and try again."
        }
    }

    private fun download(ctx: Context, url: String, onProgress: (Float) -> Unit): File {
        val out = File(ctx.cacheDir, "update.apk").also { it.delete() }
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15_000; c.readTimeout = 30_000
            c.setRequestProperty("User-Agent", "Checklist-Android")
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            val total = c.contentLengthLong
            c.inputStream.use { input ->
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024); var done = 0L
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        o.write(buf, 0, n); done += n
                        if (total > 0) onProgress(done.toFloat() / total)
                    }
                }
            }
        } finally { c.disconnect() }
        return out
    }

    /** The file must be this app and newer; Android additionally refuses it unless it has the same signing key. */
    private fun verify(ctx: Context, apk: File): String? {
        val pm = ctx.packageManager
        val dl = pm.getPackageArchiveInfo(apk.path, 0) ?: return "The downloaded file isn't a valid app."
        if (dl.packageName != ctx.packageName) return "The downloaded file is not Checklist."
        val cur = pm.getPackageInfo(ctx.packageName, 0)
        @Suppress("DEPRECATION")
        val newer = if (Build.VERSION.SDK_INT >= 28) dl.longVersionCode > cur.longVersionCode else dl.versionCode > cur.versionCode
        return if (newer) null else "The downloaded version isn't newer than this one."
    }

    private fun commit(ctx: Context, apk: File) {
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply { setAppPackageName(ctx.packageName) }
        val id = installer.createSession(params)
        installer.openSession(id).use { s ->
            apk.inputStream().use { i -> s.openWrite("update.apk", 0, apk.length()).use { o -> i.copyTo(o); s.fsync(o) } }
            val intent = Intent(ctx, InstallResultReceiver::class.java).setPackage(ctx.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            s.commit(PendingIntent.getBroadcast(ctx, id, intent, flags).intentSender)
        }
    }
}

/** Receives the installer's progress; shows Android's confirm dialog when it asks for one. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1) == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { context.startActivity(it) }
        }
    }
}
