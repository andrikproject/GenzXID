package com.genzxid.app

interface DaemonController {
    fun start()
    fun stop()
}

expect fun createDaemonController(): DaemonController
