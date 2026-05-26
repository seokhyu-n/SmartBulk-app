package com.example.smartbulk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.smartbulk.databinding.ActivityExerciseDetailBinding
import com.example.smartbulk.util.WorkoutRoutine

class ExerciseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ 루틴 데이터 받기
        val routine = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("routine", WorkoutRoutine::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("routine") as? WorkoutRoutine
        }

        if (routine != null) {
            binding.tvExerciseName.text = routine.name
            binding.tvExerciseDescription.text = routine.description
            binding.tvExerciseSets.text = routine.sets
            binding.tvExerciseReps.text = routine.reps
            binding.tvExerciseDetailDescription.text = routine.detailDescription

            routine.gifResId?.let {
                Glide.with(this).asGif().load(it).into(binding.ivExerciseGif)
            }
        } else {
            finish()
        }

        // 닫기 버튼
        binding.btnCloseDetail.setOnClickListener { finish() }

        // ✅ 자세 피드백 버튼 → PoseFeedbackActivity 실행
        binding.btnPoseFeedback.setOnClickListener {
            val intent = Intent(this, PoseFeedbackActivity::class.java)
            startActivity(intent)
        }
    }
}
