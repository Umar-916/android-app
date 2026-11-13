package com.funsol.orbit.feature.more

import com.funsol.orbit.feature.notes.NoteEditorViewModel
import com.funsol.orbit.feature.notes.NotesViewModel
import com.funsol.orbit.feature.settings.FeedbackViewModel
import com.funsol.orbit.feature.settings.RateUsViewModel
import com.funsol.orbit.feature.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val shellFeatureModule = module {
    viewModel { NotesViewModel(get()) }
    viewModel { params -> NoteEditorViewModel(get(), params.get<String?>(0), params.get<String?>(1)) }
    viewModel { SettingsViewModel(get()) }
    viewModel { FeedbackViewModel() }
    viewModel { RateUsViewModel(get()) }
}
