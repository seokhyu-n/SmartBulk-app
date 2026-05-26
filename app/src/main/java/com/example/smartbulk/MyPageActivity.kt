package com.example.smartbulk

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexboxLayout

class MyPageActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvGoal: TextView
    private lateinit var tvSignupDate: TextView
    private lateinit var tvActivityLevel: TextView
    private lateinit var tvRecentWorkoutDate: TextView
    private lateinit var tvWorkoutCount: TextView
    private lateinit var tvTotalWorkoutTime: TextView
    private lateinit var tvRoutineCount: TextView
    private lateinit var layoutTags: FlexboxLayout
    private lateinit var btnEditProfile: Button
    private lateinit var tvAppInquiry: TextView
    private lateinit var tvFAQ: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        // 뷰 초기화
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvGoal = findViewById(R.id.tvGoal)
        tvSignupDate = findViewById(R.id.tvSignupDate)
        tvActivityLevel = findViewById(R.id.tvActivityLevel)
        tvRecentWorkoutDate = findViewById(R.id.tvRecentWorkoutDate)
        tvWorkoutCount = findViewById(R.id.tvWorkoutCount)
        tvTotalWorkoutTime = findViewById(R.id.tvTotalWorkoutTime)
        tvRoutineCount = findViewById(R.id.tvRoutineCount)
        layoutTags = findViewById(R.id.layoutTags)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        tvAppInquiry = findViewById(R.id.tvAppInquiry)
        tvFAQ = findViewById(R.id.tvFAQ)

        // 내 정보 설정 화면으로 이동
        btnEditProfile.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 예시: 데이터 불러오기
        loadUserInfo()
    }

    private fun loadUserInfo() {
        // 사용자 정보 설정 (실제 데이터로 교체 필요)
        tvName.text = "이름: 홍길동"
        tvEmail.text = "이메일: hong@example.com"
        tvGoal.text = "운동 목표: 체지방 감소"
        tvSignupDate.text = "회원가입일: 2024-12-01"
        tvActivityLevel.text = "활동 레벨: 중간"
        tvRecentWorkoutDate.text = "최근 운동 완료 날짜: 2025-06-05"
        tvWorkoutCount.text = "운동 횟수: 32회"
        tvTotalWorkoutTime.text = "누적 운동 시간: 14시간"
        tvRoutineCount.text = "운동 루틴 개수: 5개"

        // 태그 추가
        layoutTags.removeAllViews()
        val exampleTags = listOf("유산소", "근력", "홈트레이닝")
        for (tag in exampleTags) {
            val tagView = TextView(this).apply {
                text = "#$tag"
                setPadding(24, 12, 24, 12)
                setTextColor(Color.WHITE)
                background = createRoundedBackground()
                setMargins(8, 8, 8, 8)
            }
            layoutTags.addView(tagView)
        }
    }

    // 배경 모양을 직접 코드로 지정
    private fun createRoundedBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(Color.parseColor("#2196F3")) // 파란 배경
        }
    }

    // 확장 함수: Margin 설정
    private fun TextView.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
        val params = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            FlexboxLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(left, top, right, bottom)
        layoutParams = params
    }
}
