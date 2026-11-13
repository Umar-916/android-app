package com.funsol.orbit.feature.projects

import com.funsol.orbit.feature.goals.GoalEditViewModel
import com.funsol.orbit.feature.goals.GoalsViewModel
import com.funsol.orbit.feature.habits.HabitDetailViewModel
import com.funsol.orbit.feature.habits.HabitEditViewModel
import com.funsol.orbit.feature.habits.HabitsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val planningFeatureModule = module {
    viewModel { ProjectsViewModel(get(), get()) }
    viewModel { (projectId: String) -> ProjectDetailViewModel(projectId, get(), get()) }
    viewModel { (projectId: String?) -> ProjectEditViewModel(projectId, get()) }

    viewModel { HabitsViewModel(get()) }
    viewModel { (habitId: String) -> HabitDetailViewModel(habitId, get()) }
    viewModel { (habitId: String?) -> HabitEditViewModel(habitId, get()) }

    viewModel { GoalsViewModel(get()) }
    viewModel { (goalId: String?) -> GoalEditViewModel(goalId, get()) }
}
