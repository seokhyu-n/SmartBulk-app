package com.example.smartbulk.util

import com.example.smartbulk.R
import com.example.smartbulk.model.UserProfile
import java.io.Serializable
import java.time.LocalDate

data class WorkoutRoutine(
    val name: String,
    val description: String,
    val sets: String,
    val reps: String,
    val detailDescription: String,
    val gifResId: Int? = null  // gif 리소스 ID 필드 추가, 기본값 null
) : Serializable

data class MuscleGroupRoutine(
    val groupName: String,
    val routines: List<WorkoutRoutine>
)

private val muscleGroupMap = mapOf(
    "근육 증가" to listOf(
        MuscleGroupRoutine(
            "가슴, 삼두", listOf(
                WorkoutRoutine(
                    "시티드 체스트프레스",
                    "가슴과 삼두근 강화",
                    "4세트",
                    "12회",
                    "발은 바닥에 평평하게 놓고 무릎은 90도 정도로 굽힙니다.\n" +
                            "손잡이를 잡을 때 손은 어깨너비보다 약간 넓게 벌립니다.\n" +
                            "팔꿈치를 완전히 펴지 말고 살짝 굽힌 상태에서 동작을 멈춥니다.",
                    gifResId = R.drawable.seated_chest_press
                ),
                WorkoutRoutine(
                    "덤벨 벤치프레스",
                    "가슴 근육 강화",
                    "4세트",
                    "10회",
                    "양팔을 완전히 펴서 덤벨을 가슴 위쪽으로 들어 올립니다.\n" +
                            "숨을 들이마시면서, 팔꿈치를 자연스럽게 굽혀 덤벨을 가슴 옆으로 천천히 내립니다.\n" +
                            "숨을 내쉬며, 가슴의 힘으로 양팔을 다시 위로 밀어 올립니다.\n" +
                            "팔꿈치가 너무 벌어지지 않도록 주의: 어깨에 부담이 갈 수 있으므로 45도 정도 각도를 유지.",
                    gifResId = R.drawable.dumbbell_benchpress
                ),
                WorkoutRoutine(
                    "케이블 푸쉬다운",
                    "삼두근 강화에 효과적",
                    "3세트",
                    "12회",
                    "바를 양손으로 어깨 너비 또는 그보다 약간 좁게 잡습니다.\n" +
                            "손바닥이 바닥을 향하게 (오버핸드 그립) 잡는 것이 일반적.\n" +
                            "몸은 살짝 앞으로 기울이되, 허리는 곧게 펴고, 팔꿈치는 몸통에 고정.\n" +
                            "어깨나 손목에 긴장을 주지 말고 삼두에 집중.",
                    gifResId = R.drawable.cable_pushdown
                ),
                WorkoutRoutine(
                    "덤벨 트라이셉스 익스텐션",
                    "삼두근 집중 운동",
                    "3세트",
                    "10회",
                    "손바닥이 덤벨 아래쪽을 받치도록 하여, 덤벨을 컵 모양으로 감싸듯이 쥡니다.\n" +
                            "두 발은 어깨 너비로 벌리고, 허리는 똑바르게 세운 채 서서 or 벤치에 앉아 준비합니다.\n" +
                            "덤벨을 머리 위로 들어올린 상태에서, 팔꿈치를 귀 옆에 붙이고 시작합니다.\n" +
                            "삼두근이 완전히 수축될 때까지 팔을 곧게 펴고, 수축을 느끼며 1초 정지하면 더 효과적입니다.",
                    gifResId = R.drawable.dumbbell_triceps_extension
                )
            )
        ),
        MuscleGroupRoutine(
            "등, 이두", listOf(
                WorkoutRoutine(
                    "바벨 로우",
                    "등 근육 강화",
                    "4세트",
                    "10회",
                    "그립 넓이는 어깨 너비 또는 약간 넓게.\n" +
                            "무릎은 살짝 굽히고, 엉덩이를 뒤로 빼며 상체를 숙입니다.\n" +
                            "허리를 곧게 펴고, 상체는 지면과 약 45도 정도로 숙인 상태를 유지합니다.\n" +
                            "항상 무게에 끌려가지 않도록 등으로 조절하며 내려야 합니다.",
                    gifResId = R.drawable.barbell_row
                ),
                WorkoutRoutine(
                    "티바 로우",
                    "등 근육 강화",
                    "3세트",
                    "12회",
                    "무릎은 살짝 굽히고, 엉덩이를 뒤로 빼며 상체를 45도 정도로 숙입니다.\n" +
                            "팔꿈치를 몸통에 붙이듯이 뒤로 당기며, 바벨을 배꼽 아래 or 명치 쪽으로 끌어옵니다.\n" +
                            "완전히 늘어지도록 내리되, 반동이나 손목 힘을 사용하지 않도록 주의하세요.",
                    gifResId = R.drawable.tbar_row
                ),
                WorkoutRoutine(
                    "해머 컬",
                    "이두 근력 강화",
                    "3세트",
                    "10회",
                    "손바닥은 서로 마주 보게 (중립 그립) 잡습니다.\n" +
                            "손목과 팔을 고정한 상태로, 덤벨을 어깨 방향으로 들어 올립니다.\n" +
                            "완전히 팔을 펴기 직전까지만 내려주세요 (긴장 유지).",
                    gifResId = R.drawable.hammer_curl
                )
            )
        ),
        MuscleGroupRoutine(
            "하체", listOf(
                WorkoutRoutine(
                    "스쿼트",
                    "하체 근육 강화",
                    "4세트",
                    "12회",
                    "발은 어깨너비로 벌립니다.\n" +
                            "발끝은 약간 바깥쪽으로 향하게 하세요 (10~15도 정도).\n" +
                            "가슴을 펴고 허리는 곧게, 시선은 정면을 향하게 유지합니다.\n" +
                            "허리는 절대 굽히지 말고, 허리-엉덩이-등이 일직선이 되도록 유지하세요.",
                    gifResId = R.drawable.squat
                ),
                WorkoutRoutine(
                    "런지",
                    "허벅지 강화",
                    "3세트",
                    "10회",
                    "양발을 엉덩이 너비로 벌리고 똑바로 선다.\n" +
                            "앞 무릎은 90도 각도, 무릎이 발끝을 넘지 않게 유지합니다.\n" +
                            "뒷무릎은 바닥에 가까워지되 닿지 않도록 내립니다.\n" +
                            "중심이 흔들리지 않도록 복부에 힘을 주세요.",
                    gifResId = R.drawable.lunge
                ),
                WorkoutRoutine(
                    "레그 프레스",
                    "하체 집중 강화",
                    "4세트",
                    "10회",
                    "발을 어깨 너비로 발판에 올립니다.\n" +
                            "다리를 펴서 중량을 지탱한 뒤 안전레버를 해제합니다.\n" +
                            "천천히 무릎을 굽혀 발판을 내립니다.\n" +
                            "다리를 힘 있게 펴면서 발판을 밀어냅니다.",
                    gifResId = R.drawable.leg_press
                )
            )
        ),
        MuscleGroupRoutine(
            "어깨", listOf(
                WorkoutRoutine(
                    "밀리터리 프레스",
                    "어깨 근육 강화",
                    "4세트",
                    "10회",
                    "어깨너비보다 약간 넓게 바벨을 잡습니다.\n" +
                            "팔꿈치를 펴면서 바벨을 머리 위로 곧게 밀어 올립니다.\n" +
                            "바는 수직으로 올라가며, 귀 옆을 지나 올라가야 합니다.\n" +
                            "등이나 허리를 과도하게 젖히지 않게 유지",
                    gifResId = R.drawable.military_press
                ),
                WorkoutRoutine(
                    "사이드 레터럴 레이즈",
                    "측면 어깨 강화",
                    "3세트",
                    "12회",
                    "양발은 어깨너비 정도로 벌리고, 무릎은 약간 굽힌 상태 유지\n" +
                            "팔꿈치를 약간 굽힌 상태에서 양팔을 옆으로 들어 올립니다.\n" +
                            "반동 없이 삼각근의 힘만으로 제어해서 내려야 효과적",
                    gifResId = R.drawable.side_lateral_raise
                )
            )
        )
    ),

    // 필요시 다른 목표도 추가 가능
)

fun generateWorkoutRoutine(user: UserProfile): List<WorkoutRoutine> {
    val routinesByGoal = muscleGroupMap[user.goal] ?: muscleGroupMap["유지"]!!

    val todayIndex = LocalDate.now().toEpochDay().toInt() % routinesByGoal.size
    val selectedGroup = routinesByGoal[todayIndex]

    val random = java.util.Random(user.name.hashCode().toLong())
    return selectedGroup.routines.shuffled(random)
}
