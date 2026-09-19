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
package com.pegoku.ophaaldag.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pegoku.ophaaldag.R
import com.pegoku.ophaaldag.data.CureData
import com.pegoku.ophaaldag.data.DataState
import com.pegoku.ophaaldag.data.UserSettings
import com.pegoku.ophaaldag.ui.components.ErrorState
import com.pegoku.ophaaldag.ui.components.FullScreenLoading
import com.pegoku.ophaaldag.ui.screens.AddressScreen
import com.pegoku.ophaaldag.ui.screens.AnnouncementDetailScreen
import com.pegoku.ophaaldag.ui.screens.AnnouncementsScreen
import com.pegoku.ophaaldag.ui.screens.CalendarScreen
import com.pegoku.ophaaldag.ui.screens.ContainersScreen
import com.pegoku.ophaaldag.ui.screens.GuideScreen
import com.pegoku.ophaaldag.ui.screens.HomeScreen
import com.pegoku.ophaaldag.ui.screens.HtmlPageScreen
import com.pegoku.ophaaldag.ui.screens.MessagesScreen
import com.pegoku.ophaaldag.ui.screens.MoreScreen
import com.pegoku.ophaaldag.ui.screens.SettingsScreen
import com.pegoku.ophaaldag.ui.screens.TipsScreen
import com.pegoku.ophaaldag.ui.screens.WasteDetailScreen
import com.pegoku.ophaaldag.ui.theme.OphaaldagTheme

enum class Tab(val labelRes: Int) {
    HOME(R.string.nav_home), CALENDAR(R.string.nav_calendar), GUIDE(R.string.nav_guide), MORE(R.string.nav_more)
}

/** Detail destinations pushed on top of the tabs. Serialised as strings so they survive process death. */
object Routes {
    const val ANNOUNCEMENTS = "announcements"
    const val MESSAGES = "messages"
    const val CONTAINERS = "containers"
    const val TIPS = "tips"
    const val FAQ = "faq"
    const val INFO = "info"
    const val SETTINGS = "settings"
    const val ADDRESS = "address"
    fun waste(type: String) = "waste/$type"
    fun announcement(id: String) = "announcement/$id"
    fun page(index: Int) = "page/$index"
}

@Composable
fun OphaaldagApp(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val dataState by vm.data.collectAsStateWithLifecycle()

    OphaaldagTheme(dynamicColor = settings?.dynamicColor ?: true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val s = settings
            when {
                s == null || !dataState.cacheLoaded -> FullScreenLoading()
                s.address == null -> AddressScreen(vm = vm, onBack = null, onDone = {})
                dataState.data == null && (dataState.loading || dataState.error == null) -> FullScreenLoading(stringResource(R.string.loading_calendar))
                dataState.data == null -> ErrorState(
                    message = errorMessage(dataState.error),
                    onRetry = { vm.refresh() },
                    secondary = { TextButton(onClick = { vm.clearAddress() }) { Text(stringResource(R.string.change_address)) } },
                )
                else -> MainScaffold(vm, s, dataState, dataState.data!!)
            }
        }
    }
}

@Composable
fun errorMessage(error: String?): String = when (error) {
    null -> stringResource(R.string.error_generic, "")
    "offline" -> stringResource(R.string.error_offline)
    else -> stringResource(R.string.error_generic, error)
}

@Composable
private fun MainScaffold(vm: AppViewModel, settings: UserSettings, state: DataState, data: CureData) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val backStack = rememberSaveable(saver = listSaver({ it.toList() }, { mutableStateListOf(*it.toTypedArray()) })) {
        mutableStateListOf<String>()
    }
    val push: (String) -> Unit = { route -> backStack.add(route) }
    val pop: () -> Unit = { if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) }
    BackHandler(enabled = backStack.isNotEmpty()) { pop() }

    val current = backStack.lastOrNull()

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = current == null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                ShortNavigationBar {
                    Tab.entries.forEach { t ->
                        val selected = t == tab
                        ShortNavigationBarItem(
                            selected = selected,
                            onClick = { tab = t },
                            icon = {
                                val icon = when (t) {
                                    Tab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                    Tab.CALENDAR -> if (selected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
                                    Tab.GUIDE -> if (selected) Icons.Filled.Search else Icons.Outlined.Search
                                    Tab.MORE -> if (selected) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz
                                }
                                Icon(icon, contentDescription = null)
                            },
                            label = { Text(stringResource(t.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = if (current == null) padding.calculateBottomPadding() else 0.dp)) {
            AnimatedContent(
                targetState = current ?: "tab:${tab.name}",
                transitionSpec = {
                    val forward = targetState != "tab:${tab.name}" && initialState.startsWith("tab:") ||
                        (!initialState.startsWith("tab:") && !targetState.startsWith("tab:") && backStack.size > 0 && targetState == backStack.lastOrNull())
                    if (initialState.startsWith("tab:") && targetState.startsWith("tab:")) {
                        fadeIn() togetherWith fadeOut()
                    } else if (forward) {
                        (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
                    }
                },
                label = "nav",
            ) { route ->
                when {
                    route == "tab:HOME" -> HomeScreen(
                        data = data, state = state,
                        onRefresh = { vm.refresh() },
                        onOpenWaste = { push(Routes.waste(it)) },
                        onOpenAnnouncement = { push(Routes.announcement(it)) },
                        onOpenAnnouncements = { push(Routes.ANNOUNCEMENTS) },
                        onOpenSettings = { push(Routes.SETTINGS) },
                    )
                    route == "tab:CALENDAR" -> CalendarScreen(data = data, onOpenWaste = { push(Routes.waste(it)) })
                    route == "tab:GUIDE" -> GuideScreen(data = data, onOpenWaste = { push(Routes.waste(it)) })
                    route == "tab:MORE" -> MoreScreen(data = data, onNavigate = push)
                    route.startsWith("waste/") -> WasteDetailScreen(data, route.removePrefix("waste/"), onBack = pop)
                    route.startsWith("announcement/") -> AnnouncementDetailScreen(data, route.removePrefix("announcement/"), onBack = pop)
                    route == Routes.ANNOUNCEMENTS -> AnnouncementsScreen(data, onBack = pop, onOpen = { push(Routes.announcement(it)) })
                    route == Routes.MESSAGES -> MessagesScreen(data, onBack = pop)
                    route == Routes.CONTAINERS -> ContainersScreen(data, onBack = pop)
                    route == Routes.TIPS -> TipsScreen(data, onBack = pop)
                    route == Routes.FAQ -> HtmlPageScreen(stringResource(R.string.faq), data.faqHtml ?: "", onBack = pop)
                    route == Routes.INFO -> HtmlPageScreen(stringResource(R.string.about_cure), data.moreInfoHtml ?: "", onBack = pop)
                    route.startsWith("page/") -> {
                        val page = data.municipalityPages.getOrNull(route.removePrefix("page/").toIntOrNull() ?: -1)
                        HtmlPageScreen(page?.title ?: "", page?.text ?: "", onBack = pop)
                    }
                    route == Routes.SETTINGS -> SettingsScreen(vm, settings, data, onBack = pop, onChangeAddress = { push(Routes.ADDRESS) })
                    route == Routes.ADDRESS -> AddressScreen(vm = vm, onBack = pop, onDone = { backStack.clear() })
                    else -> pop()
                }
            }
        }
    }
}

