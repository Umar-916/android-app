package com.funsol.orbit.feature.goals

import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.GoalStatus

enum class DeadlineKind { NONE, DAYS_LEFT, TODAY, OVERDUE }

data class Deadline(val kind: DeadlineKind, val days: Int = 0)

data class GoalSections(
    val inProgress: List<GoalEntity>,
    val completed: List<GoalEntity>,
    val archived: List<GoalEntity>,
)

object GoalsLogic {
    fun deadline(goal: GoalEntity, todayEpochDay: Int): Deadline {
        val target = goal.targetDate ?: return Deadline(DeadlineKind.NONE)
        if (goal.status != GoalStatus.ACTIVE) return Deadline(DeadlineKind.NONE)
        val diff = target - todayEpochDay
        return when {
            diff > 0 -> Deadline(DeadlineKind.DAYS_LEFT, diff)
            diff == 0 -> Deadline(DeadlineKind.TODAY)
            else -> Deadline(DeadlineKind.OVERDUE, -diff)
        }
    }

    fun sections(goals: List<GoalEntity>): GoalSections = GoalSections(
        inProgress = goals.filter { it.status == GoalStatus.ACTIVE }
            .sortedWith(compareBy<GoalEntity> { it.targetDate ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() }.thenBy { it.id }),
        completed = goals.filter { it.status == GoalStatus.COMPLETED }.sortedByDescending { it.updatedAt },
        archived = goals.filter { it.status == GoalStatus.ARCHIVED }.sortedByDescending { it.updatedAt },
    )

    fun isDateRangeValid(start: Int?, target: Int?): Boolean = start == null || target == null || target >= start
}
