package com.lasertrac.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toFormattedDateString(pattern: String = "dd-MM-yyyy"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}
