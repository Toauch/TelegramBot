package com.schedule.model

import javax.persistence.*
import java.time.DayOfWeek
import java.time.LocalTime

@Entity
@Table(name = "schedules")
data class Schedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val groupNumber: String,
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val dayOfWeek: DayOfWeek,
    
    @Column(nullable = false)
    val weekNumber: Int,
    
    @Column(nullable = false)
    val startTime: LocalTime,
    
    @Column(nullable = false)
    val endTime: LocalTime,
    
    @Column(nullable = false, length = 500)
    val subject: String,
    
    @Column(nullable = false, length = 200)
    val teacher: String,
    
    @Column(nullable = false, length = 50)
    val classroom: String
) 