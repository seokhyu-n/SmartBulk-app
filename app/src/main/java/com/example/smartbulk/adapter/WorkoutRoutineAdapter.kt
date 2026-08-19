package com.example.smartbulk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartbulk.databinding.ItemWorkoutRoutineBinding
import com.example.smartbulk.util.WorkoutRoutine

class WorkoutRoutineAdapter(
    private val routineList: List<WorkoutRoutine>,
    private val onItemClick: (WorkoutRoutine) -> Unit, // 클릭 리스너 추가
    // 체크박스를 하나씩 완료 체크할 때마다 (완료 개수, 전체 개수)를 알려준다.
    // "오늘의 운동" 화면에서만 넘겨주고, "배우고 싶은 운동" 목록에서는 기본값(아무것도 안 함)으로 둔다.
    private val onCompletionChanged: (completedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<WorkoutRoutineAdapter.RoutineViewHolder>() {

    private val completed = BooleanArray(routineList.size)

    inner class RoutineViewHolder(private val binding: ItemWorkoutRoutineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(routine: WorkoutRoutine, position: Int) {
            binding.tvRoutineName.text = routine.name
            binding.tvRoutineDescription.text = routine.description
            binding.tvRoutineSets.text = "세트: ${routine.sets}"
            binding.tvRoutineReps.text = "횟수: ${routine.reps}"

            binding.checkboxCompleted.visibility = android.view.View.VISIBLE
            // RecyclerView가 뷰를 재활용하므로, 이전 위치에 붙어있던 리스너가 지금 위치의
            // completed[]를 잘못 건드리지 않도록 값부터 세팅하고 리스너는 그 다음에 건다.
            binding.checkboxCompleted.setOnCheckedChangeListener(null)
            binding.checkboxCompleted.isChecked = completed[position]
            binding.checkboxCompleted.setOnCheckedChangeListener { _, isChecked ->
                completed[position] = isChecked
                onCompletionChanged(completed.count { it }, completed.size)
            }

            // 클릭 리스너 설정
            binding.root.setOnClickListener {
                onItemClick(routine)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val binding = ItemWorkoutRoutineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RoutineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        holder.bind(routineList[position], position)
    }

    override fun getItemCount(): Int = routineList.size

    fun isAllCompleted(): Boolean = completed.isNotEmpty() && completed.all { it }
}
