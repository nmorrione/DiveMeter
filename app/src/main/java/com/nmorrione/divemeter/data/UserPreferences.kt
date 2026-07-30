package com.nmorrione.divemeter.data

import android.content.Context

private const val PREFS_NAME = "divemeter_prefs"
private const val KEY_NICKNAME = "nickname"

/** Local device identity used to attribute saved spots once the database is shared. */
object UserPreferences {
    fun getNickname(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_NICKNAME, null)

    fun setNickname(context: Context, nickname: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NICKNAME, nickname)
            .apply()
    }
}
