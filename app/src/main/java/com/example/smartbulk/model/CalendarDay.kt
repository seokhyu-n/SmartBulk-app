package com.example.smartbulk.model

import java.time.LocalDate

data class CalendarDay(
    val date: LocalDate,       // 날짜 정보
    var isSelected: Boolean = false,  // 선택 여부 (캘린더에서 클릭 시)
    var isWorkoutDone: Boolean = false  // 운동 완료 여부 표시
)
