package com.example.smartbulk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.smartbulk.databinding.ActivityExerciseDetailBinding
import com.example.smartbulk.util.WorkoutRoutine

class ExerciseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseDetailBinding
    private var practiceMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        practiceMode = intent.getBooleanExtra(EXTRA_PRACTICE_MODE, false)

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

            // 스트레칭은 자세를 카메라로 볼 필요가 없는 동작이라 자세 피드백 버튼 자체를 숨긴다.
            binding.btnPoseFeedback.visibility =
                if (routine.name == "스트레칭") android.view.View.GONE else android.view.View.VISIBLE
        } else {
            finish()
        }

        // 닫기 버튼
        binding.btnCloseDetail.setOnClickListener { finish() }

        // ✅ 자세 피드백 버튼 → PoseFeedbackActivity 실행 (어떤 운동인지, 연습 모드인지 함께 전달)
        binding.btnPoseFeedback.setOnClickListener {
            val intent = Intent(this, PoseFeedbackActivity::class.java)
            intent.putExtra(PoseFeedbackActivity.EXTRA_EXERCISE_NAME, routine?.name)
            intent.putExtra(PoseFeedbackActivity.EXTRA_PRACTICE_MODE, practiceMode)
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_PRACTICE_MODE = "practiceMode"
    }
}
