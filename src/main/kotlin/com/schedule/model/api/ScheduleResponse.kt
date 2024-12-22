package com.schedule.model.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScheduleResponse(
    val group: String = "",
    val days: Map<String, DaySchedule> = emptyMap()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DaySchedule(
    val name: String = "",
    val lessons: List<Lesson> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Lesson(
    val teacher: String = "",
    val second_teacher: String = "",
    val subjectType: String = "",
    val week: String = "",
    val name: String = "",
    val start_time: String = "",
    val end_time: String = "",
    val start_time_seconds: Int = 0,
    val end_time_seconds: Int = 0,
    val room: String = "",
    val comment: String = "",
    val is_distant: Boolean = false,
    val temp_changes: List<TempChange> = emptyList(),
    val url: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TempChange(
    val type: String = "",
    val teacher: String? = null,
    val room: String? = null,
    val start_date: String = "",
    val end_date: String = "",
    val start_timestamp: Long = 0,
    val end_timestamp: Long = 0
) 