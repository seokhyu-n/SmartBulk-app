package com.example.smartbulk

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.smartbulk.model.UserProfile
import com.example.smartbulk.util.generateWorkoutRoutine
import com.example.smartbulk.util.WorkoutRoutine
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

    private lateinit var btnStartWorkout: MaterialButton

    private var todayWorkoutRoutine: WorkoutRoutine? = null

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

        btnStartWorkout = findViewById(R.id.btnStartWorkout)

        btnStartWorkout.setOnClickListener {
            if (todayWorkoutRoutine == null) {
                Toast.makeText(this, "추천 운동을 불러오는 중입니다. 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, WorkoutDetailActivity::class.java).apply {
                putExtra("exerciseName", todayWorkoutRoutine!!.name)
                putExtra("exerciseDescription", todayWorkoutRoutine!!.description)
                putExtra("exerciseSets", todayWorkoutRoutine!!.sets)
                putExtra("exerciseReps", todayWorkoutRoutine!!.reps)
            }
            startActivity(intent)
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

                // 추천 루틴 생성
                val userProfile = UserProfile(name, age, height, weight, goal)
                val routines = generateWorkoutRoutine(userProfile)
                todayWorkoutRoutine = routines.firstOrNull()

                if (todayWorkoutRoutine == null) {
                    Toast.makeText(this@MainActivity, "오늘의 추천 운동을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "사용자 정보를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
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
