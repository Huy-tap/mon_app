package com.example.fitnessapp.xmlui

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
            v.backgroundTintList=ColorStateList.valueOf(requireContext().getColor(if(active) R.color.ink else R.color.pale))
            v.setTextColor(requireContext().getColor(if(active) R.color.on_ink else R.color.ink))
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
