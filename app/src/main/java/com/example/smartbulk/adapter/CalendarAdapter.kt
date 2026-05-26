package com.example.smartbulk

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartbulk.databinding.CalendarDayItemBinding
import com.example.smartbulk.model.CalendarDay

class CalendarAdapter(
    private val days: List<CalendarDay>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    inner class CalendarViewHolder(private val binding: CalendarDayItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(day: CalendarDay) {
            binding.tvDay.text = day.date.dayOfMonth.toString()

            // 선택된 날짜 배경 및 텍스트 색상 강조
            if (day.isSelected) {
                binding.root.setBackgroundColor(Color.parseColor("#FFDDDD")) // 선택된 날짜 배경색
            } else {
                binding.root.setBackgroundColor(Color.TRANSPARENT)
            }

            // 운동 완료 아이콘 표시 여부
            binding.ivWorkoutDone.visibility = if (day.isWorkoutDone) View.VISIBLE else View.GONE

            // 날짜 클릭 이벤트
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
