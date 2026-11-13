package com.funsol.orbit.feature.tasks

import com.funsol.orbit.feature.calendar.CalendarViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tasksFeatureModule = module {
    viewModel { TasksViewModel(get(), get()) }
    viewModel { params ->
        TaskEditViewModel(
            taskId = params.get<String?>(0),
            presetProjectId = params.get<String?>(1),
            presetDate = params.get<Int?>(2),
            tasks = get(),
            projects = get(),
        )
    }
    viewModel { CalendarViewModel(get()) }
}
