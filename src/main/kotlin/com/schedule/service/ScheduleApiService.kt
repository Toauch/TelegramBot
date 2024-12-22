package com.schedule.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.schedule.model.Schedule
import com.schedule.model.api.ScheduleResponse
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalTime
import org.springframework.web.reactive.function.client.WebClient
import com.fasterxml.jackson.databind.JsonNode

@Service
class ScheduleApiService(
    private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()

    fun fetchSchedule(groupNumber: String): List<Schedule> {
        logger.info("Запрос расписания для группы $groupNumber")

        try {
            // Получаем расписание для каждого дня недели
            val allSchedules = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT").flatMap { day ->
                val response = webClient.get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .queryParam("weekDay", day)
                            .queryParam("groupNumber", groupNumber)
                            .build()
                    }
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()

                if (response != null && response != "{}") {
                    val jsonNode = objectMapper.readTree(response)
                    parseScheduleResponse(jsonNode, groupNumber)
                } else {
                    emptyList()
                }
            }

            return allSchedules

        } catch (e: Exception) {
            logger.error("Ошибка при получении расписания: ${e.message}", e)
            throw IllegalArgumentException("Не удалось получить расписание для группы $groupNumber: ${e.message}")
        }
    }

    private fun parseScheduleResponse(jsonNode: JsonNode, groupNumber: String): List<Schedule> {
        val schedules = mutableListOf<Schedule>()
        val groupNode = jsonNode.path(groupNumber)
        
        if (!groupNode.isMissingNode) {
            val daysNode = groupNode.path("days")
            daysNode.fields().forEach { (dayNumber, dayNode) ->
                val lessons = dayNode.path("lessons")
                lessons.forEach { lesson ->
                    try {
                        schedules.add(
                            Schedule(
                                groupNumber = groupNumber,
                                dayOfWeek = convertToDayOfWeek(dayNumber.toInt()),
                                weekNumber = lesson.path("week").asText().toIntOrNull() ?: 1,
                                startTime = LocalTime.parse(lesson.path("start_time").asText()),
                                endTime = LocalTime.parse(lesson.path("end_time").asText()),
                                subject = lesson.path("name").asText(),
                                teacher = lesson.path("teacher").asText().ifEmpty { 
                                    lesson.path("second_teacher").asText() 
                                },
                                classroom = lesson.path("room").asText()
                            )
                        )
                    } catch (e: Exception) {
                        logger.error("Ошибка при парсинге занятия: ${e.message}")
                        logger.error("Данные занятия: $lesson")
                    }
                }
            }
        }
        
        return schedules
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