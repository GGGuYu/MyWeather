package com.ixuea.course.weather.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateFormatter {
    fun getSmartDateLabel(timestamp: Long): String {
        val today = LocalDate.now()
        val targetDate = Instant.ofEpochSecond(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val monthDay = targetDate.format(DateTimeFormatter.ofPattern("M/d")) // 公共部分：月/日

        return when (ChronoUnit.DAYS.between(today, targetDate).toInt()) {
            0 -> "今天 $monthDay"
            1 -> "明天 $monthDay"
            in 2..6 -> {
                val weekday = targetDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)
                "$weekday $monthDay" // 周一 6/15
            }

            else -> monthDay // 超过6天直接显示月日
        }
    }
}