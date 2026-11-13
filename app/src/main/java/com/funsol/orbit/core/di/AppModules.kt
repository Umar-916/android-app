package com.funsol.orbit.core.di

import com.funsol.orbit.core.ads.adsModule
import com.funsol.orbit.core.backup.BackupManager
import com.funsol.orbit.core.database.OrbitDatabase
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.notifications.ReminderScheduler
import com.funsol.orbit.core.notifications.WorkManagerReminderScheduler
import com.funsol.orbit.core.repository.GoalRepository
import com.funsol.orbit.core.repository.GoalRepositoryImpl
import com.funsol.orbit.core.repository.HabitRepository
import com.funsol.orbit.core.repository.HabitRepositoryImpl
import com.funsol.orbit.core.repository.NoteRepository
import com.funsol.orbit.core.repository.NoteRepositoryImpl
import com.funsol.orbit.core.repository.ProjectRepository
import com.funsol.orbit.core.repository.ProjectRepositoryImpl
import com.funsol.orbit.core.repository.SearchRepository
import com.funsol.orbit.core.repository.SearchRepositoryImpl
import com.funsol.orbit.core.repository.TaskRepository
import com.funsol.orbit.core.repository.TaskRepositoryImpl
import com.funsol.orbit.core.startup.startupFeatureModule
import com.funsol.orbit.feature.home.dashboardFeatureModule
import com.funsol.orbit.feature.more.shellFeatureModule
import com.funsol.orbit.feature.projects.planningFeatureModule
import com.funsol.orbit.feature.tasks.tasksFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single { OrbitDatabase.create(androidContext()) }
    single { SettingsStore(androidContext()) }
    single<ReminderScheduler> { WorkManagerReminderScheduler(androidContext(), get()) }
    single<TaskRepository> { TaskRepositoryImpl(get(), get()) }
    single<ProjectRepository> { ProjectRepositoryImpl(get(), get()) }
    single<NoteRepository> { NoteRepositoryImpl(get()) }
    single<HabitRepository> { HabitRepositoryImpl(get()) }
    single<GoalRepository> { GoalRepositoryImpl(get()) }
    single<SearchRepository> { SearchRepositoryImpl(get()) }
    single { BackupManager(get(), get()) }
}

val appModules = listOf(
    coreModule,
    adsModule,
    dashboardFeatureModule,
    tasksFeatureModule,
    planningFeatureModule,
    shellFeatureModule,
    startupFeatureModule,
)
