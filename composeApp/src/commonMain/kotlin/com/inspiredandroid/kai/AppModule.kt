package com.genzxid.app

import com.genzxid.app.data.AppSettings
import com.genzxid.app.data.ConversationStorage
import com.genzxid.app.data.DataRepository
import com.genzxid.app.data.EmailStore
import com.genzxid.app.data.HeartbeatManager
import com.genzxid.app.data.MemoryStore
import com.genzxid.app.data.NotificationStore
import com.genzxid.app.data.RemoteDataRepository
import com.genzxid.app.data.SmsDraftStore
import com.genzxid.app.data.SmsStore
import com.genzxid.app.data.TaskScheduler
import com.genzxid.app.data.TaskStore
import com.genzxid.app.data.ToolExecutor
import com.genzxid.app.data.runMigrations
import com.genzxid.app.email.EmailPoller
import com.genzxid.app.inference.createLocalInferenceEngine
import com.genzxid.app.mcp.McpServerManager
import com.genzxid.app.network.Requests
import com.genzxid.app.notifications.NotificationReader
import com.genzxid.app.skills.SkillManager
import com.genzxid.app.sms.SmsPoller
import com.genzxid.app.sms.SmsReader
import com.genzxid.app.sms.SmsSender
import com.genzxid.app.splinterlands.SplinterlandsApi
import com.genzxid.app.splinterlands.SplinterlandsBattleRunner
import com.genzxid.app.splinterlands.SplinterlandsStore
import com.genzxid.app.tools.CalendarPermissionController
import com.genzxid.app.tools.NotificationListenerController
import com.genzxid.app.tools.NotificationPermissionController
import com.genzxid.app.tools.SmsPermissionController
import com.genzxid.app.tools.SmsSendPermissionController
import com.genzxid.app.ui.chat.ChatViewModel
import com.genzxid.app.ui.sandbox.SandboxFileBrowserViewModel
import com.genzxid.app.ui.sandbox.SandboxPackagesViewModel
import com.genzxid.app.ui.sandbox.SandboxSessionViewModel
import com.genzxid.app.ui.settings.SandboxViewModel
import com.genzxid.app.ui.settings.SettingsViewModel
import com.genzxid.app.ui.settings.SplinterlandsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<CalendarPermissionController> { CalendarPermissionController() }
    single<NotificationPermissionController> { NotificationPermissionController() }
    single<SmsPermissionController> { SmsPermissionController() }
    single<SmsSendPermissionController> { SmsSendPermissionController() }
    single<SmsReader> { SmsReader() }
    single<SmsSender> { SmsSender() }
    single<NotificationListenerController> { NotificationListenerController() }
    single<NotificationReader> { NotificationReader() }
    single<AppSettings> {
        AppSettings(createSecureSettings()).also {
            it.runMigrations(createLegacySettings())
        }
    }
    single<Requests> {
        Requests()
    }
    single<ConversationStorage> {
        ConversationStorage(get())
    }
    single<ToolExecutor> {
        ToolExecutor()
    }
    single<MemoryStore> {
        MemoryStore(get())
    }
    single<TaskStore> {
        TaskStore(get())
    }
    single<EmailStore> {
        EmailStore(get())
    }
    single<EmailPoller> {
        EmailPoller(get<EmailStore>())
    }
    single<SmsStore> {
        SmsStore(get())
    }
    single<SmsPoller> {
        SmsPoller(get<SmsStore>(), get<SmsReader>())
    }
    single<SmsDraftStore> {
        SmsDraftStore(get())
    }
    single<NotificationStore> {
        NotificationStore(get())
    }
    single<SplinterlandsStore> {
        SplinterlandsStore(get())
    }
    single<SplinterlandsApi> {
        SplinterlandsApi()
    }
    single<HeartbeatManager> {
        HeartbeatManager(get(), get(), get(), get())
    }
    single<McpServerManager> {
        McpServerManager(get())
    }
    single<SkillManager> {
        SkillManager(get<SandboxController>())
    }
    single<RemoteDataRepository> {
        RemoteDataRepository(
            requests = get(),
            appSettings = get(),
            conversationStorage = get(),
            toolExecutor = get(),
            memoryStore = get(),
            taskStore = get(),
            heartbeatManager = get(),
            emailStore = get(),
            emailPoller = get(),
            smsStore = get(),
            smsPoller = get(),
            smsReader = get(),
            smsPermissionController = get(),
            smsSendPermissionController = get(),
            smsSender = get(),
            smsDraftStore = get(),
            notificationStore = get(),
            notificationListenerController = get(),
            mcpServerManager = get(),
            skillManager = get(),
            sandboxController = get(),
            localInferenceEngine = createLocalInferenceEngine(),
        )
    }
    single<DataRepository> { get<RemoteDataRepository>() }
    single<SplinterlandsBattleRunner> {
        SplinterlandsBattleRunner(get(), get(), get<DataRepository>(), get<DaemonController>())
    }
    single<TaskScheduler> {
        TaskScheduler(
            get<DataRepository>(),
            get(),
            get(),
            get(),
            get(),
            get<EmailPoller>(),
            get<SmsStore>(),
            get<SmsPoller>(),
            get<NotificationStore>(),
        )
    }
    single<DaemonController> { createDaemonController() }
    single<SandboxController> { createSandboxController() }
    viewModel { SettingsViewModel(get<DataRepository>(), get<DaemonController>(), get<NotificationPermissionController>(), get<TaskScheduler>()) }
    viewModel { SandboxViewModel(get<DataRepository>(), get<SandboxController>()) }
    viewModel { SandboxFileBrowserViewModel(get<SandboxController>()) }
    viewModel { SandboxPackagesViewModel(get<SandboxController>()) }
    viewModel { SandboxSessionViewModel(get<SandboxController>(), get<DataRepository>()) }
    viewModel { SplinterlandsViewModel(get<DataRepository>(), get(), get(), get<SplinterlandsApi>()) }
    viewModel { ChatViewModel(get<DataRepository>(), get<TaskScheduler>()) }
}
