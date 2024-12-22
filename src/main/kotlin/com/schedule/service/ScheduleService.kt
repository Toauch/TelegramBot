package com.schedule.service

import com.schedule.model.Schedule
import com.schedule.repository.ScheduleRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleApiService: ScheduleApiService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun updateScheduleFromApi(groupNumber: String): String {
        return try {
            logger.info("Начало обновления расписания для группы $groupNumber")
            val schedules = scheduleApiService.fetchSchedule(groupNumber)
            logger.info("Получено ${schedules.size} занятий")
            
            scheduleRepository.deleteByGroupNumber(groupNumber)
            scheduleRepository.saveAll(schedules)
            logger.info("Расписание сохранено в базу данных")
            
            "Расписание для группы $groupNumber успешно обновлено"
        } catch (e: Exception) {
            logger.error("Ошибка при обновлении расписания для группы $groupNumber", e)
            throw e
        }
    }

    fun getNearestLesson(groupNumber: String): Schedule? {
        val now = LocalDateTime.now()
        val currentWeekNumber = getCurrentWeekNumber()
        val currentDayOfWeek = now.dayOfWeek
        val currentTime = now.toLocalTime()

        val todaySchedule = scheduleRepository.findByGroupNumberAndDayOfWeekAndWeekNumberOrderByStartTime(
            groupNumber, currentDayOfWeek, currentWeekNumber
        )

        return todaySchedule.firstOrNull { it.startTime > currentTime }
            ?: getNextDayFirstLesson(groupNumber, currentDayOfWeek, currentWeekNumber)
    }

    fun getDaySchedule(groupNumber: String, dayOfWeek: DayOfWeek, weekNumber: Int): List<Schedule> {
        logger.debug("Запрос расписания для группы $groupNumber на $dayOfWeek, неделя $weekNumber")
        return scheduleRepository.findByGroupNumberAndDayOfWeekAndWeekNumberOrderByStartTime(
            groupNumber, dayOfWeek, weekNumber
        ).also {
            logger.debug("Найдено ${it.size} занятий")
        }
    }

    fun getTomorrowSchedule(groupNumber: String): List<Schedule> {
        val tomorrow = LocalDateTime.now().plusDays(1)
        val weekNumber = getCurrentWeekNumber()
        return getDaySchedule(groupNumber, tomorrow.dayOfWeek, weekNumber)
    }

    fun getWeekSchedule(groupNumber: String, weekNumber: Int): List<Schedule> {
        return scheduleRepository.findByGroupNumberAndWeekNumber(groupNumber, weekNumber)
    }

    private fun getNextDayFirstLesson(groupNumber: String, currentDay: DayOfWeek, weekNumber: Int): Schedule? {
        var nextDay = currentDay
        var currentWeek = weekNumber

        for (i in 1..7) {
            nextDay = if (nextDay == DayOfWeek.SUNDAY) {
                currentWeek = if (currentWeek == 2) 1 else 2
                DayOfWeek.MONDAY
            } else {
                DayOfWeek.of(nextDay.value + 1)
            }

            val schedule = getDaySchedule(groupNumber, nextDay, currentWeek)
            if (schedule.isNotEmpty()) {
                return schedule.first()
            }
        }
        return null
    }

    private fun getCurrentWeekNumber(): Int {
        // Здесь можно реализовать логику определения текущей недели (1 или 2)
        // Например, на основе номера недели в году
        return LocalDateTime.now().dayOfYear / 7 % 2 + 1
    }
} 