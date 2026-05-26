package com.example.smartbulk

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smartbulk.databinding.ActivityStatisticsBinding
import com.example.smartbulk.model.CalendarDay
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var calendarAdapter: CalendarAdapter
    private val calendarDays = mutableListOf<CalendarDay>()
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val completedDates = mutableListOf<LocalDate>()

    private var currentYear = LocalDate.now().year
    private var currentMonth = LocalDate.now().monthValue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCalendar(currentYear, currentMonth)
        loadCompletedDates()

        // 월 이동 버튼
        binding.btnPrevMonth.setOnClickListener {
            moveToPreviousMonth()
        }

        binding.btnNextMonth.setOnClickListener {
            moveToNextMonth()
        }

        updateMonthTitle()
    }

    private fun setupCalendar(year: Int, month: Int) {
        calendarDays.clear()
        val daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth()

        for (day in 1..daysInMonth) {
            calendarDays.add(CalendarDay(date = LocalDate.of(year, month, day)))
        }

        calendarAdapter = CalendarAdapter(calendarDays) { clickedDay ->
            Toast.makeText(this, "선택한 날짜: ${clickedDay.date}", Toast.LENGTH_SHORT).show()
            calendarDays.forEach { it.isSelected = false }
            clickedDay.isSelected = true
            calendarAdapter.notifyDataSetChanged()
        }

        binding.calendarRecyclerView.apply {
            layoutManager = GridLayoutManager(this@StatisticsActivity, 7)
            adapter = calendarAdapter
        }

        updateCalendarWorkoutDone()
    }

    private fun loadCompletedDates() {
        val currentUser = auth.currentUser ?: return

        database.child("users").child(currentUser.uid).child("completed_dates")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    completedDates.clear()
                    for (dateSnapshot in snapshot.children) {
                        val dateStr = dateSnapshot.key ?: continue
                        try {
                            val localDate = LocalDate.parse(dateStr)
                            completedDates.add(localDate)
                        } catch (e: Exception) {
                        }
                    }

                    updateCalendarWorkoutDone()
                    updateSummaryInfo()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@StatisticsActivity, "운동 완료 날짜 불러오기 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateCalendarWorkoutDone() {
        calendarDays.forEach { day ->
            day.isWorkoutDone = completedDates.any { it == day.date }
        }
        calendarAdapter.notifyDataSetChanged()
    }

    private fun updateSummaryInfo() {
        val monthlyCount = completedDates.count { it.year == currentYear && it.monthValue == currentMonth }

        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val weeklyCount = completedDates.count { it >= monday && it <= sunday }

        val streakCount = calculateStreakCount()
        loadUserGoal()

        binding.tvMonthlyCount.text = "이번 달 총 운동 일수: ${monthlyCount}일"
        binding.tvWeeklyCount.text = "이번 주 운동 횟수: ${weeklyCount}회"
        binding.tvStreakCount.text = "연속 운동 일수: ${streakCount}일"
    }

    private fun calculateStreakCount(): Int {
        var streak = 0
        var date = LocalDate.now()
        while (completedDates.contains(date)) {
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    private fun loadUserGoal() {
        val currentUser = auth.currentUser ?: return

        database.child("users").child(currentUser.uid).child("goal")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val goal = snapshot.getValue(String::class.java) ?: "-"
                    binding.tvGoal.text = "현재 운동 목표: $goal"
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.tvGoal.text = "현재 운동 목표: -"
                }
            })
    }

    private fun moveToPreviousMonth() {
        if (currentMonth == 1) {
            currentMonth = 12
            currentYear--
        } else {
            currentMonth--
        }
        updateMonthTitle()
        setupCalendar(currentYear, currentMonth)
        updateSummaryInfo()
    }

    private fun moveToNextMonth() {
        if (currentMonth == 12) {
            currentMonth = 1
            currentYear++
        } else {
            currentMonth++
        }
        updateMonthTitle()
        setupCalendar(currentYear, currentMonth)
        updateSummaryInfo()
    }

    private fun updateMonthTitle() {
        binding.tvCurrentMonth.text = "${currentYear}년 ${currentMonth}월"
    }
}
