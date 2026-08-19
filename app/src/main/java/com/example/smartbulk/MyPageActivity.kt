package com.example.smartbulk

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexboxLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPageActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvGoal: TextView
    private lateinit var tvSignupDate: TextView
    private lateinit var tvRecentWorkoutDate: TextView
    private lateinit var tvWorkoutCount: TextView
    private lateinit var layoutTags: FlexboxLayout
    private lateinit var btnEditProfile: Button
    private lateinit var tvAppInquiry: TextView
    private lateinit var tvFAQ: TextView

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        // 뷰 초기화
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvGoal = findViewById(R.id.tvGoal)
        tvSignupDate = findViewById(R.id.tvSignupDate)
        tvRecentWorkoutDate = findViewById(R.id.tvRecentWorkoutDate)
        tvWorkoutCount = findViewById(R.id.tvWorkoutCount)
        layoutTags = findViewById(R.id.layoutTags)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        tvAppInquiry = findViewById(R.id.tvAppInquiry)
        tvFAQ = findViewById(R.id.tvFAQ)

        // 내 정보 설정 화면으로 이동
        btnEditProfile.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        loadUserInfo()
    }

    override fun onResume() {
        super.onResume()
        // SettingsActivity에서 정보를 바꾸고 돌아왔을 때도 최신 값이 보이게 매번 다시 불러온다.
        loadUserInfo()
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvEmail.text = "이메일: ${currentUser.email ?: "-"}"

        val signupMillis = currentUser.metadata?.creationTimestamp
        tvSignupDate.text = if (signupMillis != null && signupMillis > 0) {
            "회원가입일: ${formatDate(signupMillis)}"
        } else {
            "회원가입일: -"
        }

        database.child("users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "-"
                    val goal = snapshot.child("goal").getValue(String::class.java) ?: "-"
                    tvName.text = "이름: $name"
                    tvGoal.text = "운동 목표: $goal"

                    val completedDateKeys = snapshot.child("completed_dates").children
                        .mapNotNull { it.key }
                    tvWorkoutCount.text = "총 운동 완료 일수: ${completedDateKeys.size}일"
                    tvRecentWorkoutDate.text =
                        "최근 운동 완료 날짜: ${completedDateKeys.maxOrNull() ?: "기록 없음"}"

                    // "운동 스타일" 태그: 실제 태그 데이터는 없으므로, 통계 화면에서 직접 설정한
                    // 날짜별 운동 부위(dailySplit) 중 실제로 고른 부위들을 태그처럼 보여준다.
                    val usedCategories = snapshot.child("dailySplit").children
                        .mapNotNull { it.getValue(String::class.java) }
                        .filter { it != "휴식" }
                        .distinct()
                    showTags(usedCategories)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MyPageActivity, "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showTags(categories: List<String>) {
        layoutTags.removeAllViews()

        if (categories.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "통계 화면에서 날짜별 운동 부위를 설정하면 여기에 표시돼요"
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 13f
            }
            layoutTags.addView(emptyView)
            return
        }

        for (category in categories) {
            val tagView = TextView(this).apply {
                text = "#$category"
                setPadding(24, 12, 24, 12)
                setTextColor(ContextCompat.getColor(context, R.color.accent))
                background = createRoundedBackground()
                setMargins(8, 8, 8, 8)
            }
            layoutTags.addView(tagView)
        }
    }

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))

    // 배경 모양을 직접 코드로 지정
    private fun createRoundedBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(ContextCompat.getColor(this@MyPageActivity, R.color.accent_muted))
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
