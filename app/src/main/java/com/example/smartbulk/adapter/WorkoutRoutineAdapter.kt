package com.example.smartbulk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartbulk.databinding.ItemWorkoutRoutineBinding
import com.example.smartbulk.util.WorkoutRoutine

class WorkoutRoutineAdapter(
    private val routineList: List<WorkoutRoutine>,
    private val onItemClick: (WorkoutRoutine) -> Unit // 클릭 리스너 추가
) : RecyclerView.Adapter<WorkoutRoutineAdapter.RoutineViewHolder>() {

    inner class RoutineViewHolder(private val binding: ItemWorkoutRoutineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(routine: WorkoutRoutine) {
            binding.tvRoutineName.text = routine.name
            binding.tvRoutineDescription.text = routine.description
            binding.tvRoutineSets.text = "세트: ${routine.sets}"
            binding.tvRoutineReps.text = "횟수: ${routine.reps}"

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
        holder.bind(routineList[position])
    }

    override fun getItemCount(): Int = routineList.size
}
