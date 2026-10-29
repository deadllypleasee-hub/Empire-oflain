package ru.empire.game.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import ru.empire.game.MainActivity
import ru.empire.game.R
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        val state = runCatching { GameStore(c).load().state }.getOrNull()
        if (state?.settings?.notifications != true) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("empire_progress", "Развитие империи", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(c, 1, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        nm.notify(1, NotificationCompat.Builder(c, "empire_progress").setSmallIcon(R.drawable.ic_empire)
            .setContentTitle("Империя ждёт вашего решения")
            .setContentText("Вернитесь, чтобы рассчитать доход и расширить бизнес.")
            .setContentIntent(intent).setAutoCancel(true).build())
        return Result.success()
    }
    companion object {
        fun schedule(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context)
            if (!enabled) { manager.cancelUniqueWork("empire_reminder"); return }
            val request = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(8, TimeUnit.HOURS).build()
            manager.enqueueUniqueWork("empire_reminder", ExistingWorkPolicy.REPLACE, request)
        }
        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork("empire_reminder")
    }
}
