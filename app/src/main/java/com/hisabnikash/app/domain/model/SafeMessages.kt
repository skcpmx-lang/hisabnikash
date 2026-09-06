package com.hisabnikash.app.domain.model

/**
 * Last-resort error wording for saves.
 *
 * The root cause of FK failures is prevented upstream (authoritative business
 * session + repository guards), so a raw SQLite message reaching here means a
 * genuinely unexpected condition. It must still never reach the user verbatim.
 */
object SafeMessages {

    fun save(e: Throwable, fallback: String): String {
        val msg = e.message.orEmpty()
        val raw = msg.contains("SQLite", ignoreCase = true) ||
            msg.contains("FOREIGN KEY", ignoreCase = true) ||
            msg.contains("SQLITE_CONSTRAINT", ignoreCase = true)
        return if (raw || msg.isBlank()) fallback else msg
    }
}
