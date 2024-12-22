package com.schedule.repository

import com.schedule.model.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek

@Repository
interface ScheduleRepository : JpaRepository<Schedule, Long> {
    fun findByGroupNumberAndDayOfWeekAndWeekNumberOrderByStartTime(
        groupNumber: String,
        dayOfWeek: DayOfWeek,
        weekNumber: Int
    ): List<Schedule>
    
    fun findByGroupNumberAndWeekNumber(
        groupNumber: String,
        weekNumber: Int
    ): List<Schedule>

    @Modifying
    @Transactional
    @Query("DELETE FROM Schedule s WHERE s.groupNumber = :groupNumber")
    fun deleteByGroupNumber(groupNumber: String)
} 