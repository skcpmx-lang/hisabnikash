package com.hisabnikash.app.domain.model

/**
 * The ONE authoritative rule for choosing the business a write belongs to.
 *
 * Every screen keeps its own copy of "current business" for display, but the
 * ID that reaches the database can only come from the workspace session
 * (preferences), never from a form's local state and never from navigation
 * arguments. Forms historically carried businessId = 0 and wrote it straight
 * into the database — that is the shared foreign-key failure this rule kills.
 */
object BusinessContext {

    /**
     * @param preferredId      the active business id from the workspace session
     * @param preferredExists  whether that row actually exists in the database
     * @param latestId         the most recent existing business id, if any
     * @return the business id writes should use:
     *  - the preferred session id when it is a real, existing business
     *  - otherwise the latest existing business (stale-session recovery)
     *  - otherwise null => no business exists; writes must be refused
     */
    fun resolveActive(
        preferredId: Long?,
        preferredExists: Boolean,
        latestId: Long?
    ): Long? = when {
        preferredId != null && preferredId > 0 && preferredExists -> preferredId
        latestId != null && latestId > 0 -> latestId
        else -> null
    }
}
