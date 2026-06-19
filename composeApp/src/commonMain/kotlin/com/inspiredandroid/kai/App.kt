@file:OptIn(ExperimentalMaterial3Api::class)

package com.genzxid.app

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.svg.SvgDecoder
import com.genzxid.app.data.AppSettings
import com.genzxid.app.data.ThemeMode
import com.genzxid.app.tools.*
import com.genzxid.app.ui.*
import com.genzxid.app.ui.chat.ChatScreen
import com.genzxid.app.ui.chat.ChatViewModel
import com.genzxid.app.ui.components.FullScreenImageHost
import com.genzxid.app.ui.settings.SettingsScreen
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.tab_chat
import kai.composeapp.generated.resources.tab_settings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.experimental.ExperimentalVoiceApi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

// ── Navigation Routes ─────────────────────────────────────
@Serializable
@SerialName("home")
object Home

@Serializable
@SerialName("tools")
object Tools

@Serializable
@SerialName("settings")
object Settings

// ── Tab Data ──────────────────────────────────────────────
data class NavTab(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val NAV_TABS = listOf(
    NavTab(Home, "Chat", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
    NavTab(Tools, "Tools", Icons.Filled.Extension, Icons.Outlined.Extension),
    NavTab(Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

// ── Main App Composable ───────────────────────────────────
@Composable
fun App(
    navController: NavHostController,
    lightColorScheme: ColorScheme = LightColorScheme,
    darkColorScheme: ColorScheme = DarkColorScheme,
    textToSpeech: TextToSpeechInstance? = null,
    isKoinStarted: Boolean = false,
    onAppOpens: ((Int) -> Unit)? = null,
) {
    setSingletonImageLoaderFactory { context: PlatformContext ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
                add(SvgDecoder.Factory())
            }
            .build()
    }

    if (isKoinStarted) {
        AppContent(navController, lightColorScheme, darkColorScheme, textToSpeech, onAppOpens)
    } else {
        KoinApplication(
            configuration = koinConfiguration { modules(appModule) },
        ) {
            AppContent(navController, lightColorScheme, darkColorScheme, textToSpeech, onAppOpens)
        }
    }
}

// ── App Content ───────────────────────────────────────────
@Composable
private fun AppContent(
    navController: NavHostController,
    lightColorScheme: ColorScheme,
    darkColorScheme: ColorScheme,
    textToSpeech: TextToSpeechInstance?,
    onAppOpens: ((Int) -> Unit)?,
) {
    val appSettings = koinInject<AppSettings>()

    onAppOpens?.let { callback ->
        LaunchedEffect(Unit) { callback(appSettings.trackAppOpen()) }
    }

    // Permission handlers
    SetupCalendarPermissionHandler(koinInject())
    SetupNotificationPermissionHandler(koinInject())
    SetupSmsPermissionHandler(koinInject())
    SetupSmsSendPermissionHandler(koinInject())

    @OptIn(ExperimentalVoiceApi::class)
    LaunchedEffect(textToSpeech) {
        textToSpeech?.also { tts ->
            val systemLanguage = Locale.current.language
            tts.voices.firstOrNull { it.languageTag.startsWith(systemLanguage) }
                ?.let { tts.currentVoice = it }
        }
    }

    val uiScale by appSettings.uiScaleFlow.collectAsStateWithLifecycle()
    val defaultDensity = LocalDensity.current
    val scaledDensity = remember(defaultDensity, uiScale) {
        Density(defaultDensity.density * uiScale, defaultDensity.fontScale)
    }

    val themeMode by appSettings.themeModeFlow.collectAsStateWithLifecycle()
    val systemInDark = isSystemInDarkTheme()
    val effectiveColorScheme = when (themeMode) {
        ThemeMode.System -> if (systemInDark) darkColorScheme else lightColorScheme
        ThemeMode.Light -> lightColorScheme
        ThemeMode.Dark -> darkColorScheme
        ThemeMode.OledBlack -> darkColorScheme.withBlackBackground()
    }

    val sandboxController = koinInject<SandboxController>()
    val sandboxAwareUriHandler = rememberSandboxAwareUriHandler(sandboxController)

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalUriHandler provides sandboxAwareUriHandler,
    ) {
        Theme(colorScheme = effectiveColorScheme) {
            FullScreenImageHost {
                val chatViewModel: ChatViewModel = koinViewModel()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                // Detect platform
                val isMobile = currentPlatform is Platform.Mobile
                val isDesktop = currentPlatform is Platform.Desktop

                // Single full-screen aurora backdrop behind every layout. All
                // surfaces above (nav bars, screens) stay translucent so the
                // flowing gradient bleeds gently through (Aurora + Glassmorphism).
                Box(Modifier.fillMaxSize().auroraBackground()) {
                    if (isDesktop) {
                        // ── DESKTOP: Sidebar Layout ──────────
                        DesktopLayout(navController, currentRoute, chatViewModel, textToSpeech)
                    } else {
                        // ── MOBILE: Bottom Bar Layout ─────────
                        MobileLayout(navController, currentRoute, chatViewModel, textToSpeech)
                    }
                }
            }
        }
    }
}

// ── MOBILE: Bottom Navigation Layout ─────────────────────
@Composable
private fun MobileLayout(
    navController: NavHostController,
    currentRoute: String?,
    chatViewModel: ChatViewModel,
    textToSpeech: TextToSpeechInstance?,
) {
    val selectedTab = NAV_TABS.indexOfFirst { it.route::class.qualifiedName == currentRoute }
        .coerceAtLeast(0)

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                tonalElevation = 0.dp,
            ) {
                NAV_TABS.forEachIndexed { index, tab ->
                    val selected = index == selectedTab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHostContent(navController, chatViewModel, textToSpeech)
        }
    }
}

// ── DESKTOP: Sidebar (NavigationRail) Layout ──────────────
@Composable
private fun DesktopLayout(
    navController: NavHostController,
    currentRoute: String?,
    chatViewModel: ChatViewModel,
    textToSpeech: TextToSpeechInstance?,
) {
    val selectedTab = NAV_TABS.indexOfFirst { it.route::class.qualifiedName == currentRoute }
        .coerceAtLeast(0)

    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar
        NavigationRail(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            header = {
                // Logo area
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(darkPurple, darkCyan)),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "G",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    )
                }
                Spacer(Modifier.height(24.dp))
            },
        ) {
            Spacer(Modifier.weight(1f))
            NAV_TABS.forEachIndexed { index, tab ->
                val selected = index == selectedTab
                NavigationRailItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(tab.route) {
                            popUpTo(Home) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                        )
                    },
                    label = {
                        Text(tab.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    ),
                )
            }
            Spacer(Modifier.weight(1f))
        }

        // Main content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            NavHostContent(navController, chatViewModel, textToSpeech)
        }
    }
}

// ── NavHost Content ────────────────────────────────────────
@Composable
private fun NavHostContent(
    navController: NavHostController,
    chatViewModel: ChatViewModel,
    textToSpeech: TextToSpeechInstance?,
) {
    NavHost(
        navController,
        startDestination = Home,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        composable<Home> {
            ChatScreen(
                viewModel = chatViewModel,
                textToSpeech = textToSpeech,
                onNavigateToSettings = {
                    navController.navigate(Settings)
                },
                isSandboxAvailable = currentPlatform is Platform.Mobile.Android,
                navigationTabBar = null,
            )
        }
        composable<Tools> {
            // Tools screen placeholder — same as Chat for now
            ChatScreen(
                viewModel = chatViewModel,
                textToSpeech = textToSpeech,
                onNavigateToSettings = {
                    navController.navigate(Settings)
                },
                isSandboxAvailable = currentPlatform is Platform.Mobile.Android,
                navigationTabBar = null,
            )
        }
        composable<Settings> {
            SettingsScreen(
                onNavigateBack = {
                    chatViewModel.refreshSettings()
                    navController.navigateUp()
                },
                navigationTabBar = null,
            )
        }
    }
}
