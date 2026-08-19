package com.example.smartbulk

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartbulk.R
import com.example.smartbulk.databinding.CalendarDayItemBinding
import com.example.smartbulk.model.CalendarDay

class CalendarAdapter(
    private val days: List<CalendarDay>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    inner class CalendarViewHolder(private val binding: CalendarDayItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(day: CalendarDay) {
            // date가 null이면 월 시작 요일을 맞추기 위한 빈 칸 — 아무것도 그리지 않고 클릭도 막는다
            if (day.date == null) {
                binding.tvDay.text = ""
                binding.ivWorkoutDone.visibility = View.GONE
                binding.root.setBackgroundColor(Color.TRANSPARENT)
                binding.root.setOnClickListener(null)
                binding.root.isClickable = false
                return
            }

            binding.tvDay.text = day.date.dayOfMonth.toString()

            // 선택된 날짜 배경 및 텍스트 색상 강조
            if (day.isSelected) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.accent_muted)
                )
            } else {
                binding.root.setBackgroundColor(Color.TRANSPARENT)
            }

            // 운동 완료 아이콘 표시 여부
            binding.ivWorkoutDone.visibility = if (day.isWorkoutDone) View.VISIBLE else View.GONE

            // 날짜 클릭 이벤트
            binding.root.isClickable = true
            binding.root.setOnClickListener {
                onDayClick(day)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val binding = CalendarDayItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CalendarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size
}
