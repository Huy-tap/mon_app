package com.example.fitnessapp.data

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.fitnessapp.MainActivity
import com.example.fitnessapp.R
import com.example.fitnessapp.model.*
import java.util.concurrent.Executors

/** Source scheduler extended with exact access, channel checks and stale-alarm validation. */
object ReminderScheduler {
    const val CHANNEL="workout_reminders"
    const val FIRE="com.example.fitnessapp.REMINDER"
    val executor = Executors.newSingleThreadExecutor()
    fun channel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL,"Nhắc nhở tập luyện",NotificationManager.IMPORTANCE_DEFAULT))
    }
    fun permitted(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    fun exact(context: Context): Boolean = Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun schedule(context: Context, r: Reminder) {
        channel(context)
        val manager=context.getSystemService(AlarmManager::class.java)
        val intent=Intent(context,ReminderReceiver::class.java).setAction(FIRE)
        val pending=PendingIntent.getBroadcast(context,41,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.cancel(pending)
        val prefs=context.getSharedPreferences("alarm_delivery",Context.MODE_PRIVATE)
        prefs.edit().remove("due").remove("signature").commit()
        val next=ReminderRules.next(r) ?: return
        if(!permitted(context)) return
        val due=next.toInstant().toEpochMilli()
        val signature=ReminderRules.signature(r)
        intent.putExtra("due",due).putExtra("signature",signature)
        val alarm=PendingIntent.getBroadcast(context,41,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        prefs.edit().putLong("due",due).putString("signature",signature).commit()
        try {
            if(exact(context)) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due,alarm)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due,alarm)
        } catch(_: SecurityException) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due,alarm) }
    }
    fun restore(context: Context) { executor.execute {
        try { schedule(context,FitnessRepository(FitnessDatabase.open(context)).getPrimaryReminder()) }
        catch(e: Exception) { Log.e("ReminderScheduler","Cannot restore alarm",e) }
    } }
    fun notify(context: Context) {
        if(!permitted(context)) return
        val tap=PendingIntent.getActivity(context,42,Intent(context,MainActivity::class.java).putExtra("reminder",true),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val message="Hãy dành thời gian cho buổi tập hôm nay. Chỉ cần 15 phút để duy trì phong độ!"
        context.getSystemService(NotificationManager::class.java).notify(41,NotificationCompat.Builder(context,CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Đến giờ tập luyện! 💪").setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)).setContentIntent(tap).setAutoCancel(true).build())
    }
}
class ReminderReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val pending=goAsync()
        ReminderScheduler.executor.execute {
            try {
                val r=FitnessRepository(FitnessDatabase.open(context)).getPrimaryReminder()
                if(intent.action==ReminderScheduler.FIRE) {
                    val prefs=context.getSharedPreferences("alarm_delivery",Context.MODE_PRIVATE)
                    val due=intent.getLongExtra("due",-1)
                    val valid=r.isEnabled && due>0 && due<=System.currentTimeMillis() && due==prefs.getLong("due",0) &&
                        intent.getStringExtra("signature")==ReminderRules.signature(r) && prefs.getString("signature",null)==ReminderRules.signature(r)
                    if(!valid) return@execute // A stale delivery must not replace a newly scheduled alarm.
                    prefs.edit().remove("due").commit()
                    try { ReminderScheduler.notify(context) }
                    finally { ReminderScheduler.schedule(context,r) }
                    return@execute
                }
                ReminderScheduler.schedule(context,r)
            } catch(e: Exception) { Log.e("ReminderReceiver","Cannot process reminder",e) }
            finally { pending.finish() }
        }
    }
}
