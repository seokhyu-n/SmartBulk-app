package com.example.smartbulk

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.smartbulk.databinding.ItemWorkoutRoutineBinding
import com.example.smartbulk.model.UserProfile
import com.example.smartbulk.util.routineForToday
import com.example.smartbulk.util.learnableExercises
import com.example.smartbulk.util.WorkoutRoutine
import com.example.smartbulk.util.DietRecommender
import java.time.LocalDate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar

    private lateinit var nameTextView: TextView
    private lateinit var ageTextView: TextView
    private lateinit var heightTextView: TextView
    private lateinit var weightTextView: TextView
    private lateinit var goalTextView: TextView
    private lateinit var textTodayRoutine: TextView
    private lateinit var textTodayMeal: TextView

    private lateinit var btnStartWorkout: MaterialButton
    private lateinit var learnExercisesContainer: LinearLayout

    private var todayRoutines: List<WorkoutRoutine>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        toolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)

        nameTextView = findViewById(R.id.textName)
        ageTextView = findViewById(R.id.textAge)
        heightTextView = findViewById(R.id.textHeight)
        weightTextView = findViewById(R.id.textWeight)
        goalTextView = findViewById(R.id.textGoal)
        textTodayRoutine = findViewById(R.id.textTodayRoutine)
        textTodayMeal = findViewById(R.id.textTodayMeal)

        btnStartWorkout = findViewById(R.id.btnStartWorkout)
        learnExercisesContainer = findViewById(R.id.learnExercisesContainer)
        populateLearnExercises()

        btnStartWorkout.setOnClickListener {
            if (todayRoutines == null) {
                Toast.makeText(this, "추천 운동을 불러오는 중입니다. 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startActivity(Intent(this, WorkoutDetailActivity::class.java))
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_my_page -> {
                    startActivity(Intent(this, MyPageActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // onCreate가 아니라 onResume에서 불러야, 통계 화면에서 날짜별 계획을 바꾸고
        // 뒤로가기로 이 화면에 돌아왔을 때(= onCreate는 다시 안 불림) 최신 값으로 갱신된다.
        loadUserProfileAndGenerateRoutine()
    }

    private fun loadUserProfileAndGenerateRoutine() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                val age = snapshot.child("age").getValue(Int::class.java)
                val height = snapshot.child("height").getValue(Int::class.java)
                val weight = snapshot.child("weight").getValue(Int::class.java)
                val goal = snapshot.child("goal").getValue(String::class.java)

                if (name == null || age == null || height == null || weight == null || goal == null) {
                    Toast.makeText(this@MainActivity, "프로필 정보가 불완전합니다.", Toast.LENGTH_SHORT).show()
                    return
                }

                // 사용자 정보 표시
                nameTextView.text = "안녕하세요, $name 님!"
                ageTextView.text = "나이: ${age}세"
                heightTextView.text = "키: ${height}cm"
                weightTextView.text = "몸무게: ${weight}kg"
                goalTextView.text = "운동 목표: $goal"

                // 날짜별 운동 부위 설정(통계 화면에서 저장한 값)을 같이 읽어서
                // '운동 시작하기'와 완전히 같은 기준으로 오늘의 루틴을 만든다.
                val dailySplit = snapshot.child("dailySplit").children.associate {
                    (it.key ?: "") to (it.getValue(String::class.java) ?: "휴식")
                }

                val userProfile = UserProfile(name, age, height, weight, goal)
                val routines = routineForToday(userProfile, dailySplit)
                todayRoutines = routines

                textTodayRoutine.text = routines.joinToString("\n") { "• ${it.name} — ${it.sets} ${it.reps}" }

                loadTodayDietRecommendation()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "사용자 정보를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // AI(Claude)에게 오늘 운동 부위/프로필에 맞는 식단을 추천받는다.
    // 실제 API 호출은 Cloud Functions 쪽에서 하고, 같은 날짜면 서버에 캐시된 결과를 재사용하므로
    // 이 화면을 여러 번 열어도 비용이 반복해서 들지 않는다.
    private fun loadTodayDietRecommendation() {
        textTodayMeal.text = "불러오는 중..."
        DietRecommender.fetchTodayRecommendation(
            date = LocalDate.now(),
            onSuccess = { recommendation ->
                textTodayMeal.text = recommendation.toDisplayText()
            },
            onFailure = { exception ->
                textTodayMeal.text = DietRecommender.friendlyErrorMessage(exception)
            }
        )
    }

    // 자세 분석을 지원하는 운동 전부를 카드로 쌓는다. ScrollView 안에 RecyclerView를 wrap_content로
    // 중첩시키면 높이 계산이 불안정해서 일부만 보이는 문제가 있어, 항목 수가 적은(10개 안팎) 이 목록은
    // RecyclerView 없이 뷰를 직접 추가하는 방식으로 확실하게 전부 보이게 했다.
    private fun populateLearnExercises() {
        learnExercisesContainer.removeAllViews()
        learnableExercises().forEach { routine ->
            val itemBinding = ItemWorkoutRoutineBinding.inflate(layoutInflater, learnExercisesContainer, false)
            itemBinding.tvRoutineName.text = routine.name
            itemBinding.tvRoutineDescription.text = routine.description
            itemBinding.tvRoutineSets.text = "세트: ${routine.sets}"
            itemBinding.tvRoutineReps.text = "횟수: ${routine.reps}"
            itemBinding.root.setOnClickListener {
                val intent = Intent(this, ExerciseDetailActivity::class.java)
                intent.putExtra("routine", routine)
                intent.putExtra(ExerciseDetailActivity.EXTRA_PRACTICE_MODE, true)
                startActivity(intent)
            }
            learnExercisesContainer.addView(itemBinding.root)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_drawer_toggle -> {
                drawerLayout.openDrawer(GravityCompat.END)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
