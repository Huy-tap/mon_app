package com.example.fitnessapp.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.*
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.YearMonth
import java.util.Locale

class MonthSheet: BottomSheetDialogFragment() {
    private var selected=YearMonth.now()
    override fun onCreate(state: Bundle?) { super.onCreate(state);selected=YearMonth.parse(state?.getString("month") ?: requireArguments().getString("month")) }
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,state: Bundle?): View {
        val b=SheetMonthBinding.inflate(inflater,container,false)
        val buttons=listOf(b.month1,b.month2,b.month3,b.month4,b.month5,b.month6,b.month7,b.month8,b.month9,b.month10,b.month11,b.month12)
        fun render() { b.year.text="Năm ${selected.year}";buttons.forEachIndexed { i,v ->
            val active=i+1==selected.monthValue
            v.backgroundTintList=ColorStateList.valueOf(Color.parseColor(if(active) "#111827" else "#F1F5F9"))
            v.setTextColor(Color.parseColor(if(active) "#FFFFFF" else "#111827"))
        } }
        b.previousYear.asset(R.raw.chevron_left);b.nextYear.asset(R.raw.chevron_right)
        b.previousYear.setOnClickListener { if(selected.year>1) selected=selected.minusYears(1);render() }
        b.nextYear.setOnClickListener { if(selected.year<9999) selected=selected.plusYears(1);render() }
        buttons.forEachIndexed { i,v -> v.setOnClickListener { selected=YearMonth.of(selected.year,i+1);render() } }
        b.cancel.setOnClickListener { dismiss() }
        b.confirm.setOnClickListener { parentFragmentManager.setFragmentResult("month",Bundle().apply { putString("month",selected.toString()) });dismiss() }
        render();return b.root
    }
    override fun onSaveInstanceState(out: Bundle) { super.onSaveInstanceState(out);out.putString("month",selected.toString()) }
}
class TimeSheet: BottomSheetDialogFragment() {
    private var binding: SheetTimeBinding?=null
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,state: Bundle?): View {
        val b=SheetTimeBinding.inflate(inflater,container,false);binding=b
        b.hour.setText(state?.getString("hour") ?: requireArguments().getString("time")!!.substring(0,2))
        b.minute.setText(state?.getString("minute") ?: requireArguments().getString("time")!!.substring(3,5))
        b.cancel.setOnClickListener { dismiss() }
        b.confirm.setOnClickListener {
            val h=b.hour.text.toString().toIntOrNull();val m=b.minute.text.toString().toIntOrNull()
            b.hour.error=if(h==null || h !in 0..23) "Giờ từ 00 đến 23" else null
            b.minute.error=if(m==null || m !in 0..59) "Phút từ 00 đến 59" else null
            if(h!=null && h in 0..23 && m!=null && m in 0..59) {
                parentFragmentManager.setFragmentResult("time",Bundle().apply { putString("time",String.format(Locale.ROOT,"%02d:%02d:00",h,m)) });dismiss()
            }
        }
        return b.root
    }
    override fun onSaveInstanceState(out: Bundle) { super.onSaveInstanceState(out);out.putString("hour",binding?.hour?.text.toString());out.putString("minute",binding?.minute?.text.toString()) }
    override fun onDestroyView() { binding=null;super.onDestroyView() }
}
