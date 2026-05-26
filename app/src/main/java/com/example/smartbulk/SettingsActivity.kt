package com.example.smartbulk

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var etGoalWeight: EditText
    private lateinit var btnSaveWeight: Button

    private lateinit var spinnerExerciseGoal: Spinner
    private lateinit var btnChangeGoal: Button

    private lateinit var spinnerWorkoutFrequency: Spinner
    private lateinit var btnSaveFrequency: Button

    private lateinit var tvAppVersion: TextView
    private lateinit var tvCopyright: TextView
    private lateinit var tvContact: TextView

    private val exerciseGoals = listOf("근육 증가", "체중 유지", "체중 감소")
    private val workoutFrequencies = (1..7).map { "$it 회/주" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etGoalWeight = findViewById(R.id.etGoalWeight)
        btnSaveWeight = findViewById(R.id.btnSaveWeight)

        spinnerExerciseGoal = findViewById(R.id.spinnerExerciseGoal)
        btnChangeGoal = findViewById(R.id.btnChangeGoal)

        spinnerWorkoutFrequency = findViewById(R.id.spinnerWorkoutFrequency)
        btnSaveFrequency = findViewById(R.id.btnSaveFrequency)

        tvAppVersion = findViewById(R.id.tvAppVersion)
        tvCopyright = findViewById(R.id.tvCopyright)
        tvContact = findViewById(R.id.tvContact)

        // 앱 버전 정보 고정 텍스트로 표시
        tvAppVersion.text = "앱 버전: 1.0.0"

        // 카피라이트 문구 및 연락처(홈페이지 링크) 설정
        tvCopyright.text = "© 2025 SmartBulk. All rights reserved."
        tvContact.text = "문의: support@smartbulk.com"

        // 스피너 어댑터 설정
        spinnerExerciseGoal.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, exerciseGoals)
        spinnerWorkoutFrequency.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, workoutFrequencies)

        loadSettings()

        btnSaveWeight.setOnClickListener {
            val weightStr = etGoalWeight.text.toString()
            if (weightStr.isNotEmpty()) {
                val weight = weightStr.toFloatOrNull()
                if (weight != null && weight > 0) {
                    saveGoalWeight(weight)
                    Toast.makeText(this, "목표 체중이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "유효한 숫자를 입력하세요.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "목표 체중을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        btnChangeGoal.setOnClickListener {
            val selectedGoal = spinnerExerciseGoal.selectedItem as String
            saveExerciseGoal(selectedGoal)
            Toast.makeText(this, "운동 목표가 변경되었습니다.", Toast.LENGTH_SHORT).show()
        }

        btnSaveFrequency.setOnClickListener {
            val selectedFrequencyStr = spinnerWorkoutFrequency.selectedItem as String
            val frequency = selectedFrequencyStr.split(" ")[0].toIntOrNull()
            if (frequency != null && frequency in 1..7) {
                saveWorkoutFrequency(frequency)
                Toast.makeText(this, "운동 빈도가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "운동 빈도를 선택하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        val goalWeight = prefs.getFloat("goal_weight", 0f)
        if (goalWeight > 0f) {
            etGoalWeight.setText(goalWeight.toString())
        }

        val exerciseGoal = prefs.getString("exercise_goal", exerciseGoals[0])
        val exerciseGoalIndex = exerciseGoals.indexOf(exerciseGoal)
        if (exerciseGoalIndex >= 0) {
            spinnerExerciseGoal.setSelection(exerciseGoalIndex)
        }

        val workoutFrequency = prefs.getInt("workout_frequency", 3) // 기본 주 3회
        val frequencyIndex = workoutFrequencies.indexOf("$workoutFrequency 회/주")
        if (frequencyIndex >= 0) {
            spinnerWorkoutFrequency.setSelection(frequencyIndex)
        }
    }

    private fun saveGoalWeight(weight: Float) {
        val prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        prefs.edit().putFloat("goal_weight", weight).apply()
    }

    private fun saveExerciseGoal(goal: String) {
        val prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("exercise_goal", goal).apply()
    }

    private fun saveWorkoutFrequency(frequency: Int) {
        val prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        prefs.edit().putInt("workout_frequency", frequency).apply()
    }
}
