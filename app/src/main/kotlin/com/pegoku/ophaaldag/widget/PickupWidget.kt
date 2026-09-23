/*
 * Ophaaldag - a Material 3 client for the Cure Afvalbeheer waste calendar.
 * Copyright (C) 2026 Pere Gomila
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.pegoku.ophaaldag.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pegoku.ophaaldag.OphaaldagApplication
import com.pegoku.ophaaldag.MainActivity
import com.pegoku.ophaaldag.R
import com.pegoku.ophaaldag.data.CureData
import com.pegoku.ophaaldag.data.PickupDay
import com.pegoku.ophaaldag.data.WasteTypes
import com.pegoku.ophaaldag.util.Dates
import java.time.LocalDate

class PickupWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = OphaaldagApplication.from(context)
        app.repository.awaitCache()
        val data = app.repository.state.value.data
        val upcoming = app.repository.upcoming(app.settings.current().collectedBy, limit = 4)
        provideContent {
            GlanceTheme {
                WidgetContent(context, data, upcoming)
            }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, data: CureData?, upcoming: List<PickupDay>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            text = context.getString(R.string.widget_label),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(GlanceModifier.height(6.dp))
        when {
            data == null -> Text(context.getString(R.string.widget_no_address), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
            upcoming.isEmpty() -> Text(context.getString(R.string.no_upcoming), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
            else -> {
                val today = LocalDate.now()
                upcoming.forEachIndexed { index, day ->
                    val date = day.localDate ?: return@forEachIndexed
                    val style = WasteTypes.style(day.type)
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = GlanceModifier.size(10.dp).cornerRadius(5.dp).background(ColorProvider(style.color)),
                        ) {}
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = data.labelFor(day.type),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = if (index == 0) 16.sp else 14.sp,
                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                            ),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Text(
                            text = Dates.relativeDay(context, date, today),
                            style = TextStyle(color = if (index == 0) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                        )
                    }
                }
            }
        }
    }
}

