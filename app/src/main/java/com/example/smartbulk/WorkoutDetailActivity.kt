package com.example.smartbulk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartbulk.adapter.WorkoutRoutineAdapter
import com.example.smartbulk.databinding.ActivityWorkoutDetailBinding
import com.example.smartbulk.model.UserProfile
import com.example.smartbulk.util.routineForToday
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutDetailBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private var workoutAdapter: WorkoutRoutineAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerViewWorkout.layoutManager = LinearLayoutManager(this)
        loadUserProfileAndRecommend()
    }

    private fun loadUserProfileAndRecommend() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "사용자 로그인 필요", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userRef = database.child("users").child(currentUser.uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "이름 없음"
                    val age = snapshot.child("age").getValue(Int::class.java) ?: 0
                    val height = snapshot.child("height").getValue(Int::class.java) ?: 0
                    val weight = snapshot.child("weight").getValue(Int::class.java) ?: 0
                    val goal = snapshot.child("goal").getValue(String::class.java) ?: "유지"
                    val dailySplit = snapshot.child("dailySplit").children.associate {
                        (it.key ?: "") to (it.getValue(String::class.java) ?: "휴식")
                    }

                    val userProfile = UserProfile(name, age, height, weight, goal)
                    showRecommendedWorkout(userProfile, dailySplit)
                } else {
                    Toast.makeText(this@WorkoutDetailActivity, "사용자 프로필을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@WorkoutDetailActivity, "프로필 불러오기 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun showRecommendedWorkout(userProfile: UserProfile, dailySplit: Map<String, String>) {
        val routines = routineForToday(userProfile, dailySplit)

        if (routines.isNotEmpty()) {
            // 운동을 하나씩 체크할 때마다 전부 체크됐는지 확인해서, 다 체크해야만
            // "운동 완료" 버튼이 활성화되게 한다.
            val adapter = WorkoutRoutineAdapter(
                routineList = routines,
                onItemClick = { routine ->
                    val intent = Intent(this, ExerciseDetailActivity::class.java)
                    intent.putExtra("routine", routine)
                    startActivity(intent)
                },
                onCompletionChanged = { completedCount, totalCount ->
                    binding.btnCompleteWorkout.isEnabled = completedCount == totalCount
                }
            )
            workoutAdapter = adapter
            binding.recyclerViewWorkout.adapter = adapter
            binding.btnCompleteWorkout.isEnabled = false
        } else {
            Toast.makeText(this, "추천 운동이 없습니다", Toast.LENGTH_SHORT).show()
        }

        binding.btnCompleteWorkout.setOnClickListener {
            if (workoutAdapter?.isAllCompleted() != true) {
                Toast.makeText(this, "모든 운동을 완료 체크해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())

                database.child("users").child(currentUser.uid).child("completed_dates").child(today)
                    .setValue(true)
                    .addOnSuccessListener {
                        Toast.makeText(this, "운동 완료 저장됨", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "운동 완료 저장 실패", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
