package com.example.libvlc_custom.player.utils

import java.util.*
import java.util.concurrent.TimeUnit

object TimeUtils {

    /**
     * Get the number of hours from milliseconds.
     *
     * @param milliseconds Time in milliseconds.
     * @return Time in hours.
     */
    fun getHours(milliseconds: Long): Long {
        return TimeUnit.MILLISECONDS.toHours(milliseconds)
    }

    /**
     * Get the number of minutes from milliseconds.
     *
     * @param milliseconds Time in milliseconds.
     * @return Time in minutes.
     */
    fun getMinutes(milliseconds: Long): Long {
        return TimeUnit.MILLISECONDS.toMinutes(milliseconds)
    }

    /**
     * Get the number of seconds from milliseconds.
     *
     * @param milliseconds Time in milliseconds.
     * @return Time in seconds.
     */
    fun getSeconds(milliseconds: Long): Long {
        return TimeUnit.MILLISECONDS.toSeconds(milliseconds)
    }


    /**
     * Get a formatted timestamp from milliseconds.
     *
     * @param milliseconds Time in milliseconds.
     * @return A formatted timestamp.
     */
    fun getTimeString(milliseconds: Long): String {
        var milliseconds = milliseconds
        if (milliseconds < 0) {
            return "0"
        }
        val hours = getHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = getMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = getSeconds(milliseconds)
        if (hours == 0L && minutes == 0L) {
            return String.format(
                Locale.US,
                "0:%02d",
                seconds
            )
        }
        return if (hours == 0L) {
            String.format(
                Locale.US,
                "%d:%02d",
                minutes,
                seconds
            )
        } else String.format(
            Locale.US,
            "%d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
    }
}