package hissab.assistant.pk

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class annotated with [HiltAndroidApp] so Hilt can generate the
 * application-level dependency graph used by activities, view models and
 * repositories.
 */
@HiltAndroidApp
class HisaabAssistantApp : Application()
