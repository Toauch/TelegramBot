package com.schedule.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.schedule.model.Schedule
import com.schedule.model.api.ScheduleResponse
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalTime

@Service
class LocalScheduleService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()
    private val scheduleData: Map<String, ScheduleResponse> by lazy {
        val resource = ClassPathResource("schedule_data.json")
        objectMapper.readValue(resource.inputStream)
    }

    fun fetchSchedule(groupNumber: String): List<Schedule> {
        val formattedGroupNumber = "${groupNumber}1"
        logger.info("Получение расписания из локального файла для группы $formattedGroupNumber")
        
        val response = scheduleData[formattedGroupNumber] 
            ?: throw IllegalArgumentException("Группа $groupNumber не найдена")

        return response.days.flatMap { (dayNumber, daySchedule) ->
            daySchedule.lessons.map { lesson ->
                Schedule(
                    groupNumber = groupNumber,
                    dayOfWeek = convertToDayOfWeek(dayNumber.toInt()),
                    weekNumber = lesson.week.toIntOrNull() ?: 1,
                    startTime = LocalTime.parse(lesson.start_time),
                    endTime = LocalTime.parse(lesson.end_time),
                    subject = lesson.name,
                    teacher = lesson.teacher.ifEmpty { lesson.second_teacher },
                    classroom = lesson.room
                )
            }
        }
    }

    private fun convertToDayOfWeek(dayNumber: Int): DayOfWeek {
        return when (dayNumber) {
            0 -> DayOfWeek.MONDAY
            1 -> DayOfWeek.TUESDAY
            2 -> DayOfWeek.WEDNESDAY
            3 -> DayOfWeek.THURSDAY
            4 -> DayOfWeek.FRIDAY
            5 -> DayOfWeek.SATURDAY
            6 -> DayOfWeek.SUNDAY
            else -> throw IllegalArgumentException("Неверный номер дня недели: $dayNumber")
        }
    }
} 