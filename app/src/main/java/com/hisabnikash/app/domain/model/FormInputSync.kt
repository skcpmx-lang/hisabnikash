package com.hisabnikash.app.domain.model

/**
 * Pure rule for edit forms loaded from the database.
 *
 * A form must seed its fields from the stored record exactly ONCE. After that,
 * in-progress user edits always win — a reactive database emission must never
 * overwrite what the user is typing.
 */
object FormInputSync {

    /**
     * @param loaded         whether the form has already been seeded
     * @param current        the current form values (what the user sees/types)
     * @param stored         the persisted record, if one exists
     * @param currentIsBlank true when the user has not typed anything yet
     * @return (loaded, values to display): a new form is seeded from [stored]
     * exactly once; every later call keeps [current], so typing is never lost.
     * If the user typed before the record finished loading, their input wins
     * and the form is marked loaded so the database cannot overwrite it.
     */
    fun <T> mergeLoaded(
        loaded: Boolean,
        current: T,
        stored: T?,
        currentIsBlank: Boolean = true
    ): Pair<Boolean, T> = when {
        loaded || stored == null -> loaded to current
        !currentIsBlank -> true to current
        else -> true to stored
    }
}
