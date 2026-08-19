package com.example.smartbulk

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smartbulk.databinding.ActivityStatisticsBinding
import com.example.smartbulk.model.CalendarDay
import com.example.smartbulk.util.WEEKLY_SPLIT_CATEGORIES
import com.example.smartbulk.util.routinesForCategory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private val KOREAN_WEEKDAY = mapOf(
    DayOfWeek.MONDAY to "월", DayOfWeek.TUESDAY to "화", DayOfWeek.WEDNESDAY to "수",
    DayOfWeek.THURSDAY to "목", DayOfWeek.FRIDAY to "금", DayOfWeek.SATURDAY to "토", DayOfWeek.SUNDAY to "일"
)

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var calendarAdapter: CalendarAdapter
    private val calendarDays = mutableListOf<CalendarDay>()
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val completedDates = mutableListOf<LocalDate>()

    // 달력에서 아무 날짜나 눌렀을 때 그날 계획을 보여주기 위한 전체 날짜별 설정 캐시(이번 주뿐 아니라 전체)
    private val dailySplitCache = mutableMapOf<String, String>()
    private var userGoal = "근육 증가"

    private var currentYear = LocalDate.now().year
    private var currentMonth = LocalDate.now().monthValue

    // 이번 주 월~일 각 날짜와 그 날짜의 라벨/스피너를 짝지어서 로드/저장 코드를 반복 없이 처리.
    // 날짜 자체(예: "2026-08-17")를 키로 쓰기 때문에, 다음 주가 되면 사용자가 다시 와서 설정해야 한다 —
    // 요일마다 영원히 반복되는 규칙이 아니라 "이번 주는 이렇게 할래" 라는 실제 계획에 가깝게 만든 것.
    private val dateSlots: List<Triple<LocalDate, TextView, Spinner>> by lazy {
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val labels = listOf(
            binding.tvDateLabelMon, binding.tvDateLabelTue, binding.tvDateLabelWed,
            binding.tvDateLabelThu, binding.tvDateLabelFri, binding.tvDateLabelSat, binding.tvDateLabelSun
        )
        val spinners = listOf(
            binding.spinnerMon, binding.spinnerTue, binding.spinnerWed,
            binding.spinnerThu, binding.spinnerFri, binding.spinnerSat, binding.spinnerSun
        )
        (0..6).map { offset ->
            val date = monday.plusDays(offset.toLong())
            Triple(date, labels[offset], spinners[offset])
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCalendar(currentYear, currentMonth)
        loadCompletedDates()
        setupDailySplitEditor()
        loadDailySplitCache()

        // 월 이동 버튼
        binding.btnPrevMonth.setOnClickListener {
            moveToPreviousMonth()
        }

        binding.btnNextMonth.setOnClickListener {
            moveToNextMonth()
        }

        updateMonthTitle()
    }

    private fun setupDailySplitEditor() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, WEEKLY_SPLIT_CATEGORIES)
        dateSlots.forEach { (date, label, spinner) ->
            spinner.adapter = adapter
            label.text = "${date.dayOfMonth}(${KOREAN_WEEKDAY[date.dayOfWeek]})"
        }

        loadDailySplit()

        binding.btnSaveWeeklySplit.setOnClickListener {
            saveDailySplit()
        }
    }

    private fun loadDailySplit() {
        val currentUser = auth.currentUser ?: return

        database.child("users").child(currentUser.uid).child("dailySplit")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dateSlots.forEach { (date, _, spinner) ->
                        val saved = snapshot.child(date.toString()).getValue(String::class.java)
                        val index = WEEKLY_SPLIT_CATEGORIES.indexOf(saved).let { if (it < 0) WEEKLY_SPLIT_CATEGORIES.lastIndex else it }
                        spinner.setSelection(index) // 저장된 값이 없으면 마지막 항목("휴식")을 기본값으로
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@StatisticsActivity, "날짜별 설정 불러오기 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveDailySplit() {
        val currentUser = auth.currentUser ?: return

        val split = dateSlots.associate { (date, _, spinner) -> date.toString() to spinner.selectedItem.toString() }

        database.child("users").child(currentUser.uid).child("dailySplit")
            .updateChildren(split)
            .addOnSuccessListener {
                dailySplitCache.putAll(split) // 화면을 새로 열지 않아도 바로 달력 탭에 반영되게
                Toast.makeText(this, "날짜별 운동 부위가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // 달력에서 아무 날짜나 눌러도 그날 계획을 보여줄 수 있도록, 이번 주뿐 아니라 저장된 날짜 전체를 불러온다.
    private fun loadDailySplitCache() {
        val currentUser = auth.currentUser ?: return

        database.child("users").child(currentUser.uid).child("dailySplit")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dailySplitCache.clear()
                    for (child in snapshot.children) {
                        val key = child.key ?: continue
                        val value = child.getValue(String::class.java) ?: continue
                        dailySplitCache[key] = value
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupCalendar(year: Int, month: Int) {
        calendarDays.clear()
        val firstOfMonth = LocalDate.of(year, month, 1)
        val daysInMonth = firstOfMonth.lengthOfMonth()

        // 월 시작 요일에 맞춰 빈 칸을 앞에 채워서, 위의 요일 헤더(월~일)와 실제 날짜 칸이 같은 열에 오게 한다.
        // DayOfWeek.value: 월=1 ... 일=7 이므로 그대로 빈 칸 개수가 된다.
        val leadingBlanks = firstOfMonth.dayOfWeek.value - 1
        repeat(leadingBlanks) { calendarDays.add(CalendarDay(date = null)) }

        for (day in 1..daysInMonth) {
            calendarDays.add(CalendarDay(date = LocalDate.of(year, month, day)))
        }

        calendarAdapter = CalendarAdapter(calendarDays) { clickedDay ->
            calendarDays.forEach { it.isSelected = false }
            clickedDay.isSelected = true
            calendarAdapter.notifyDataSetChanged()
            showRoutineForDate(clickedDay.date)
        }

        binding.calendarRecyclerView.apply {
            layoutManager = GridLayoutManager(this@StatisticsActivity, 7)
            adapter = calendarAdapter
        }

        updateCalendarWorkoutDone()
    }

    // 달력에서 날짜를 누르면 그날 계획된 부위와 운동 목록을 다이얼로그로 보여준다.
    private fun showRoutineForDate(date: LocalDate?) {
        if (date == null) return

        val category = dailySplitCache[date.toString()] ?: "휴식"
        val routines = routinesForCategory(userGoal, category)
        val label = "${date.monthValue}/${date.dayOfMonth}(${KOREAN_WEEKDAY[date.dayOfWeek]})"

        val body = routines.joinToString("\n") { "• ${it.name} — ${it.sets} ${it.reps}" }

        MaterialAlertDialogBuilder(this, R.style.SmartBulk_AlertDialog)
            .setTitle("$label · $category")
            .setMessage(body)
            .setPositiveButton("확인", null)
            .show()
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
                    if (goal != "-") userGoal = goal
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
