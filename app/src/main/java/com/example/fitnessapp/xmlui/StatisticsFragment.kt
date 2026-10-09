package com.example.fitnessapp.xmlui

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitnessapp.*
import com.example.fitnessapp.data.*
import com.example.fitnessapp.databinding.FragmentStatisticsBinding
import kotlinx.coroutines.*
import java.time.YearMonth
import java.util.Locale

class StatisticsFragment: Fragment() {
    private var binding: FragmentStatisticsBinding?=null
    private var month=YearMonth.now()
    private var job: Job?=null
    override fun onCreate(state: Bundle?) { super.onCreate(state);state?.getString("month")?.let { month=YearMonth.parse(it) } }
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,state: Bundle?): View = FragmentStatisticsBinding.inflate(inflater,container,false).also { binding=it }.root
    override fun onViewCreated(view: View,state: Bundle?) {
        val b=binding!!
        b.reminders.asset(R.raw.bell);b.previous.asset(R.raw.chevron_left);b.next.asset(R.raw.chevron_right)
        b.reminders.setOnClickListener { (requireActivity() as StatisticsReminderXmlActivity).openReminder() }
        b.previous.setOnClickListener { month=month.minusMonths(1);load() }
        b.next.setOnClickListener { month=month.plusMonths(1);load() }
        b.month.setOnClickListener { MonthSheet().apply { arguments=Bundle().apply { putString("month",month.toString()) } }.show(childFragmentManager,"month") }
        childFragmentManager.setFragmentResultListener("month",viewLifecycleOwner) { _,result -> month=YearMonth.parse(result.getString("month"));load() }
        b.retry.setOnClickListener { load() }
        b.startWorkout.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), R.string.statistics_workout_unavailable, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() { super.onResume();load() }
    private fun load() {
        val b=binding ?: return
        job?.cancel()
        b.month.text=String.format(Locale.ROOT,"Tháng %02d/%d",month.monthValue,month.year)
        b.summary.text="Tháng ${month.monthValue} của bạn"
        b.loading.isVisible=true;b.cards.isVisible=false;b.stateCard.isVisible=false;b.chartCard.isVisible=false;b.summary.isVisible=true
        val requested=month
        job=viewLifecycleOwner.lifecycleScope.launch {
            try {
                val context=requireContext().applicationContext
                val (stats, frequency)=withContext(Dispatchers.IO) {
                    val repo=FitnessRepository(FitnessDatabase.open(context))
                    repo.monthStats(requested) to repo.monthFrequency(requested)
                }
                b.frequencyChart.setCounts(frequency)
                b.chartCard.isVisible=stats.sessions>0L
                b.startWorkout.isVisible=stats.sessions==0L
                b.sessions.text=stats.sessions.toString();b.minutes.text=stats.minutes.toString();b.completed.text=stats.completedExercises.toString()
                b.cards.isVisible=true;b.stateCard.isVisible=stats.sessions==0L;b.retry.isVisible=false
                b.stateIcon.asset(R.raw.empty);b.stateTitle.text="";b.stateTitle.isVisible=false
                (b.stateMessage.layoutParams as ViewGroup.MarginLayoutParams).topMargin=(16*resources.displayMetrics.density).toInt()
                b.stateMessage.text="Chưa có dữ liệu tập luyện trong tháng này"
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) {
                b.cards.isVisible=false;b.chartCard.isVisible=false;b.summary.isVisible=false;b.startWorkout.isVisible=false;b.stateCard.isVisible=true;b.retry.isVisible=true;b.stateTitle.isVisible=true
                (b.stateMessage.layoutParams as ViewGroup.MarginLayoutParams).topMargin=(8*resources.displayMetrics.density).toInt()
                b.stateIcon.asset(R.raw.error);b.stateTitle.text="Lỗi tải dữ liệu";b.stateMessage.text="Không thể tải dữ liệu. Vui lòng thử lại."
            } finally { if (isActive) b.loading.isVisible=false }
        }
    }
    override fun onSaveInstanceState(out: Bundle) { super.onSaveInstanceState(out);out.putString("month",month.toString()) }
    override fun onDestroyView() { job?.cancel();binding=null;super.onDestroyView() }
}
