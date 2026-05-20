package llc.lookatwhataicando.codeoba.core.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Logger {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    fun log(msg: String) {
        val timestamp = LocalDateTime.now().format(formatter)
        println("[$timestamp] $msg")
    }

    fun log(msg: String, throwable: Throwable) {
        val timestamp = LocalDateTime.now().format(formatter)
        println("[$timestamp] $msg")
        throwable.printStackTrace()
    }
}
