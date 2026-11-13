package com.funsol.orbit.feature.premium

import com.funsol.orbit.core.datastore.AppSettings

fun isPremiumUser(settings: AppSettings): Boolean = settings.isPremium
