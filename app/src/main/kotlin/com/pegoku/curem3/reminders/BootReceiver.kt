package com.pegoku.curem3.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pegoku.curem3.CureApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = CureApplication.from(context)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.repository.awaitCache()
                ReminderScheduler.reschedule(context, app.repository.state.value.data, app.settings.current())
            } finally {
                pending.finish()
            }
        }
    }
}
