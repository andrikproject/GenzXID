package com.genzxid.app.notifications

import com.genzxid.app.data.NotificationRecord

actual class NotificationReader actual constructor() {
    actual fun isSupported(): Boolean = false
    actual fun hasAccess(): Boolean = false
    actual suspend fun getById(id: String): NotificationRecord? = null
    actual suspend fun search(query: String, limit: Int, packageName: String?): List<NotificationRecord> = emptyList()
}
