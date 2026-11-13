package com.funsol.orbit.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyStateShowsMessageAndRunsAction() {
        var clicks = 0
        compose.setContent {
            OrbitTheme {
                EmptyState(Icons.Rounded.Checklist, "No tasks yet", "Create your first task.", actionLabel = "Add task", onAction = { clicks++ })
            }
        }
        compose.onNodeWithText("No tasks yet").assertIsDisplayed()
        compose.onNodeWithText("Add task").performClick()
        assertEquals(1, clicks)
    }

    @Test fun confirmDialogOffersAVerbAndCancel() {
        var confirmed = false
        var dismissed = false
        compose.setContent {
            OrbitTheme {
                ConfirmDialog("Delete project?", "Project “Launch” will be removed.", "Delete project", { confirmed = true }, { dismissed = true }, destructive = true, dismissLabel = "Cancel")
            }
        }
        compose.onNodeWithText("Project “Launch” will be removed.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(true, dismissed)
        compose.onNodeWithText("Delete project").performClick()
        assertEquals(true, confirmed)
    }

    @Test fun progressRingShowsItsLabel() {
        compose.setContent { OrbitTheme { ProgressRing(0.5f, "50%") } }
        compose.onNodeWithText("50%").assertIsDisplayed()
    }
}
