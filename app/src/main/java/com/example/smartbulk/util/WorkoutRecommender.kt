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
                ),
                WorkoutRoutine(
                    "풀업",
                    "등 전체 근력과 악력 강화",
                    "4세트",
                    "최대한 많이",
                    "봉을 어깨너비보다 약간 넓게 오버핸드 그립으로 잡습니다.\n" +
                            "어깨를 아래로 내려 견갑골을 모은 상태에서 몸을 끌어올립니다.\n" +
                            "턱이 봉 위로 올라올 때까지 당기고, 팔을 완전히 펼 때까지 천천히 내려옵니다.\n" +
                            "반동(스윙) 없이 등 근육의 힘으로 당기는 데 집중하세요."
                    // gif 리소스 아직 없음 — 추후 res/drawable에 gif 추가되면 gifResId만 채우면 됨
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

/** 날짜별 운동 부위 설정(통계 화면)에서 고를 수 있는 선택지. "휴식"이면 유산소·스트레칭만 나온다. */
val WEEKLY_SPLIT_CATEGORIES = listOf("가슴, 삼두", "등, 이두", "하체", "어깨", "휴식")

private val cardioRoutine = WorkoutRoutine(
    "유산소",
    "심폐지구력과 체지방 감소에 도움",
    "1세트",
    "20분",
    "빠르게 걷기, 조깅, 사이클 중 하나를 선택해 20분간 진행합니다.\n" +
            "대화가 가능한 정도의 강도(중강도)를 유지하세요."
)

private val stretchingBefore = WorkoutRoutine(
    "스트레칭",
    "부상 예방을 위한 준비 스트레칭",
    "운동 전",
    "몸 풀어주기 10분",
    "본운동 전에 목, 어깨, 허리, 다리 순서로 각 부위를 15~20초씩 가볍게 늘려줍니다.\n" +
            "차가운 근육을 다치지 않게, 반동 없이 천천히 진행하세요."
)

private val stretchingAfter = WorkoutRoutine(
    "스트레칭",
    "회복을 위한 마무리 스트레칭",
    "운동 후",
    "몸 풀어주기 10분",
    "본운동 후에 사용한 근육 위주로 15~20초씩 늘려 회복을 돕습니다.\n" +
            "호흡을 편안하게 유지하며, 통증이 없는 범위까지만 진행하세요."
)

/**
 * 목표(goal)와 부위(category)에 맞는 운동 목록을 만든다.
 * 순서는 [준비 스트레칭 → 부위 운동 → 유산소 → 마무리 스트레칭]으로 고정해서
 * 항상 운동 전후로 스트레칭이 끼워지게 한다. "휴식"이거나 해당 부위 그룹을 찾을 수 없으면
 * 부위 운동 없이 [준비 스트레칭 → 유산소 → 마무리 스트레칭]만 반환한다.
 * 통계 화면에서 특정 날짜를 눌러 그날 계획을 미리 보여줄 때도 이 함수를 그대로 쓴다.
 */
fun routinesForCategory(goal: String, category: String): List<WorkoutRoutine> {
    if (category == "휴식") return listOf(stretchingBefore, cardioRoutine, stretchingAfter)

    val groupsForGoal = muscleGroupMap[goal] ?: muscleGroupMap.values.first()
    val group = groupsForGoal.firstOrNull { it.groupName == category }
        ?: return listOf(stretchingBefore, cardioRoutine, stretchingAfter)

    return listOf(stretchingBefore) + group.routines + cardioRoutine + stretchingAfter
}

/**
 * 사용자가 통계 화면에서 날짜별로 정해둔 부위(dailySplit, 키는 "2026-08-17" 같은 날짜 문자열)에
 * 맞는 오늘의 운동 목록을 만든다.
 * "오늘의 추천 루틴"(메인 화면 카드)과 "운동 시작하기"(WorkoutDetailActivity)가
 * 항상 이 함수 하나만 써서 같은 운동을 보여주게 한다.
 */
fun routineForToday(
    user: UserProfile,
    dailySplit: Map<String, String>,
    date: LocalDate = LocalDate.now()
): List<WorkoutRoutine> {
    val category = dailySplit[date.toString()] ?: "휴식"
    return routinesForCategory(user.goal, category)
}

/**
 * 카메라 자세 분석(SUPPORTED_EXERCISE_NAMES)을 지원하는 운동만 모아, "배우고 싶은 운동" 목록 등에 쓴다.
 * 등록된 운동 원본은 muscleGroupMap에 있는 것을 그대로 재사용한다(설명/GIF 중복 방지).
 */
fun learnableExercises(): List<WorkoutRoutine> {
    val all = muscleGroupMap.values.flatten().flatMap { it.routines }
    return com.example.smartbulk.SUPPORTED_EXERCISE_NAMES.mapNotNull { name ->
        all.firstOrNull { it.name == name }
    }
}
