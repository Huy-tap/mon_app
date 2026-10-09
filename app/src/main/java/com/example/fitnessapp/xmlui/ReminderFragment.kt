package com.example.fitnessapp.xmlui

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitnessapp.R
import com.example.fitnessapp.data.*
import com.example.fitnessapp.databinding.FragmentReminderBinding
import com.example.fitnessapp.model.*
import kotlinx.coroutines.*
import java.time.format.DateTimeFormatter

class ReminderFragment: Fragment() {
    private var binding: FragmentReminderBinding?=null
    private var draft: Reminder?=null
    private var saved: Reminder?=null
    private var rendering=false
    private var busy=false
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        ReminderScheduler.restore(requireContext().applicationContext);renderStatus()
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        state?.getBundle("draft")?.let { draft=decode(it) }
        state?.getBundle("saved")?.let { saved=decode(it) }
    }
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,state: Bundle?): View = FragmentReminderBinding.inflate(inflater,container,false).also { binding=it }.root
    override fun onViewCreated(view: View,state: Bundle?) {
        val b=binding!!
        b.back.asset(R.raw.back);b.clock.asset(R.raw.clock);b.changeTime.icon=svg(requireContext(),R.raw.edit)
        b.back.setOnClickListener { (requireActivity() as StatisticsReminderXmlActivity).closeReminder() }
        b.enabled.setOnCheckedChangeListener { _,checked -> if(!rendering) { draft=draft?.copy(isEnabled=checked);renderStatus() } }
        b.daily.setOnClickListener { draft=draft?.copy(repeatType="DAILY");render() }
        b.weekly.setOnClickListener { draft=draft?.copy(repeatType="WEEKLY");render() }
        days().forEach { chip -> chip.setOnCheckedChangeListener { _,_ -> if(!rendering) {
            draft=draft?.copy(repeatDays=days().mapIndexedNotNull { i,v -> if(v.isChecked) i+1 else null }.joinToString(","))
        } } }
        b.changeTime.setOnClickListener { draft?.let { TimeSheet().apply { arguments=Bundle().apply { putString("time",it.reminderTime) } }.show(childFragmentManager,"time") } }
        childFragmentManager.setFragmentResultListener("time",viewLifecycleOwner) { _,result -> draft=draft?.copy(reminderTime=result.getString("time")!!);render() }
        b.permission.setOnClickListener {
            if(Build.VERSION.SDK_INT>=33 && !requireContext().getSharedPreferences("permission",0).getBoolean("asked",false)) {
                requireContext().getSharedPreferences("permission",0).edit().putBoolean("asked",true).apply()
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,requireContext().packageName))
        }
        b.exactAccess.setOnClickListener {
            if(Build.VERSION.SDK_INT>=31) openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:${requireContext().packageName}")))
        }
        b.save.setOnClickListener { save() }
        render()
        if(draft==null) load()
    }
    private fun openSettings(intent: Intent) { runCatching { startActivity(intent) }.onFailure { binding?.feedback?.text="Không thể mở cài đặt hệ thống." } }
    private fun days()=binding!!.let { listOf(it.day1,it.day2,it.day3,it.day4,it.day5,it.day6,it.day7) }
    private fun load() {
        busy=true;render()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val context=requireContext().applicationContext
                saved=withContext(Dispatchers.IO) { FitnessRepository(FitnessDatabase.open(context)).getPrimaryReminder() }
                draft=saved;binding?.feedback?.text=""
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { binding?.feedback?.text="Không thể tải lịch nhắc. Chạm Thử lại." }
            finally { busy=false;render() }
        }
    }
    private fun save() {
        if(draft==null) { load();return }
        val value=draft!!
        if(value.repeatType=="WEEKLY" && reminderDays(value.repeatDays).isEmpty()) { binding?.feedback?.text="Chọn ít nhất một ngày trong tuần.";return }
        busy=true;render()
        // Application-owned executor serializes saves with broadcast processing; it survives rotation.
        val context=requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {
                    ReminderScheduler.executor.submit<Pair<Reminder,Boolean>> {
                        val r=FitnessRepository(FitnessDatabase.open(context)).saveReminder(value)
                        r to runCatching { ReminderScheduler.schedule(context,r) }.isSuccess
                    }.get()
                }
                saved=result.first;draft=result.first
                binding?.feedback?.text=if(result.second) "Đã lưu cài đặt." else "Đã lưu lịch, nhưng chưa đặt được báo thức. Hãy mở lại ứng dụng."
                if(value.isEnabled && Build.VERSION.SDK_INT>=33 && !ReminderScheduler.permitted(context) && !context.getSharedPreferences("permission",0).getBoolean("asked",false)) {
                    context.getSharedPreferences("permission",0).edit().putBoolean("asked",true).apply();permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { binding?.feedback?.text="Không lưu được cài đặt. Vui lòng thử lại." }
            finally { busy=false;render() }
        }
    }
    private fun render() {
        val b=binding ?: return
        rendering=true
        val r=draft
        b.enabled.isEnabled=r!=null && !busy;b.daily.isEnabled=r!=null && !busy;b.weekly.isEnabled=r!=null && !busy;b.changeTime.isEnabled=r!=null && !busy
        b.save.isEnabled=!busy;b.save.text=if(busy) "Đang xử lý…" else if(r==null) "Thử lại" else "Lưu cài đặt"
        if(r!=null) {
            b.enabled.isChecked=r.isEnabled;b.time.text=r.reminderTime.take(5)
            b.daySection.isVisible=r.repeatType=="WEEKLY"
            b.legacy.isVisible=r.repeatType=="ONCE";b.legacy.text="Lịch một lần: ${r.scheduledDate}. Chọn chế độ để đổi lịch."
            listOf(b.daily to "DAILY",b.weekly to "WEEKLY").forEach { (button,type) ->
                val active=r.repeatType==type
                button.backgroundTintList=ColorStateList.valueOf(Color.parseColor(if(active) "#1E3A8A" else "#FFFFFF"))
                button.setTextColor(Color.parseColor(if(active) "#FFFFFF" else "#6B7280"))
            }
            days().forEachIndexed { i,v -> v.isChecked=i+1 in reminderDays(r.repeatDays);v.isEnabled=!busy }
        }
        rendering=false;renderStatus()
    }
    private fun renderStatus() {
        val b=binding ?: return
        ReminderScheduler.channel(requireContext())
        val allowed=ReminderScheduler.permitted(requireContext())
        b.warning.isVisible=!allowed
        b.toggleStatus.text=if(draft?.isEnabled==true) "Đã bật cấu hình${if(!allowed) " • Chưa hoạt động" else ""}" else "Đang tắt"
        val r=saved
        val schedule=when(r?.repeatType) { "WEEKLY" -> "Theo tuần (${reminderDays(r.repeatDays).joinToString { if(it==7) "CN" else "T${it+1}" }})";"ONCE" -> "Một lần ${r.scheduledDate}";else -> "Hằng ngày" }
        val next=r?.let { ReminderRules.next(it) }
        b.savedStatus.text=when {
            r==null -> "Đang tải lịch đã lưu…"
            r.id==0L -> "Chưa thiết lập lịch"
            !r.isEnabled -> "Lịch đã lưu: $schedule lúc ${r.reminderTime.take(5)}\nĐang tắt"
            !allowed -> "Lịch đã lưu: $schedule lúc ${r.reminderTime.take(5)}\nChưa hoạt động • Thông báo đang bị chặn"
            next==null -> "Lịch đã lưu: $schedule lúc ${r.reminderTime.take(5)}\nKhông có lần nhắc tiếp theo"
            else -> "Lịch đã lưu: $schedule lúc ${r.reminderTime.take(5)}\nLần nhắc tiếp theo: ${next.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}"
        }
        val exact=ReminderScheduler.exact(requireContext())
        b.exactAccess.isVisible=!exact
        b.exactStatus.text=if(exact) "Đã cho phép nhắc đúng giờ" else "Chưa cho phép báo thức chính xác. Lời nhắc có thể đến trễ."
    }
    override fun onResume() {
        super.onResume();renderStatus()
        if(draft!=null) {
            val context=requireContext().applicationContext
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    saved=withContext(Dispatchers.IO) {
                        ReminderScheduler.executor.submit<Reminder> { FitnessRepository(FitnessDatabase.open(context)).getPrimaryReminder() }.get()
                    }
                    renderStatus()
                } catch(e: CancellationException) { throw e }
                catch(_: Exception) { binding?.feedback?.text="Không thể cập nhật trạng thái lịch đã lưu." }
            }
        }
    }
    override fun onSaveInstanceState(out: Bundle) { super.onSaveInstanceState(out);draft?.let { out.putBundle("draft",encode(it)) };saved?.let { out.putBundle("saved",encode(it)) } }
    override fun onDestroyView() { binding=null;super.onDestroyView() }
    private fun encode(r: Reminder)=Bundle().apply {
        putLong("id",r.id);putString("title",r.title);putString("message",r.message)
        putString("time",r.reminderTime);putString("type",r.repeatType);putBoolean("enabled",r.isEnabled)
        putString("days",r.repeatDays);putString("date",r.scheduledDate)
    }
    private fun decode(b: Bundle)=Reminder(b.getLong("id"),b.getString("title")!!,b.getString("message"),b.getString("time")!!,b.getString("type")!!,b.getBoolean("enabled"),b.getString("days"),b.getString("date"))
}
