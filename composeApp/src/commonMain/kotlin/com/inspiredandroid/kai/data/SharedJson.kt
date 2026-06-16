package com.genzxid.app.data

import kotlinx.serialization.json.Json

val SharedJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
