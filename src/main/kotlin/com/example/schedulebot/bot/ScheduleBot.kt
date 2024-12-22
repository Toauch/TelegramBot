import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ScheduleBot(
    private val scheduleApiService: ScheduleApiService
) {
    fun getScheduleForDate(date: LocalDate): String {
        val schedule = scheduleApiService.getSchedule(date)
        return schedule.joinToString("\n") { 
            "${it.subject} (${it.startTime} - ${it.endTime})\n" +
            "Аудитория: ${it.room}\n" +
            "Преподаватель: ${it.teacher}\n"
        }
    }
} 