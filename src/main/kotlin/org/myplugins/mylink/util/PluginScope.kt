package org.myplugins.mylink.util

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PluginScope(name: String) {

    private val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.IO + job + CoroutineName(name))

    fun cancel() = job.cancel()
}