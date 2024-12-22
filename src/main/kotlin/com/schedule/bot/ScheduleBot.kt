package com.schedule.bot

import com.schedule.config.BotConfig
import com.schedule.service.ScheduleService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

@Component
class ScheduleBot(
    private val botConfig: BotConfig,
    private val scheduleService: ScheduleService
) : TelegramLongPollingBot() {

    private val userStates = mutableMapOf<Long, UserState>()
    private val daysOfWeek = setOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday")

    override fun getBotUsername(): String = botConfig.username

    override fun getBotToken(): String = botConfig.token

    override fun onUpdateReceived(update: Update) {
        if (!update.hasMessage() || !update.message.hasText()) return

        val chatId = update.message.chatId
        val messageText = update.message.text.lowercase()

        val response = when {
            messageText.startsWith("/start") -> {
                userStates.remove(chatId)
                getStartMessage()
            }
            messageText == "update" -> {
                userStates[chatId] = UserState(waitingForGroup = true)
                "Введите номер группы"
            }
            userStates[chatId]?.waitingForGroup == true -> {
                val result = processUpdateSchedule("update $messageText")
                userStates[chatId] = UserState(groupNumber = messageText)
                result
            }
            daysOfWeek.contains(messageText) -> {
                val state = userStates[chatId]
                if (state?.groupNumber != null) {
                    userStates[chatId] = state.copy(day = messageText)
                    "Введите номер недели (1 или 2)"
                } else {
                    "Сначала используйте команду update и введите номер группы"
                }
            }
            userStates[chatId]?.day != null && messageText.matches(Regex("^[12]$")) -> {
                val state = userStates[chatId]!!
                val result = processDaySchedule("${state.day} ${messageText} ${state.groupNumber}")
                userStates[chatId] = state.copy(day = null)
                result
            }
            messageText == "near_lesson" -> {
                val state = userStates[chatId]
                if (state?.groupNumber != null) {
                    processNearLesson("near_lesson ${state.groupNumber}")
                } else {
                    "Сначала используйте команду update и введите номер группы"
                }
            }
            messageText == "tomorrow" -> {
                val state = userStates[chatId]
                if (state?.groupNumber != null) {
                    processTomorrowSchedule("tomorrow ${state.groupNumber}")
                } else {
                    "Сначала используйте команду update и введите номер группы"
                }
            }
            messageText.startsWith("all") -> processWeekSchedule(messageText)
            else -> "Неизвестная команда. Используйте /start для получения списка команд."
        }

        sendNotification(chatId.toString(), response)
    }

    private fun processUpdateSchedule(message: String): String {
        val parts = message.split(" ")
        if (parts.size != 2) return "Неверный формат команды. Используйте: update GROUP_NUMBER"

        val groupNumber = parts[1]
        return try {
            scheduleService.updateScheduleFromApi(groupNumber)
        } catch (e: Exception) {
            "Ошибка при обновлении расписания: ${e.message}"
        }
    }

    private fun getStartMessage(): String {
        return """
            Доступные команды:
            update - обновить расписание (затем введите номер группы)
            
            После обновления расписания используйте:
            - Кнопки дней недели для просмотра расписания
            - near_lesson для просмотра ближайшего занятия
            - tomorrow для просмотра расписания на завтра
            
            Дополнительные команды:
            all WEEK_NUMBER GROUP_NUMBER - расписание на всю неделю
        """.trimIndent()
    }

    private fun processNearLesson(message: String): String {
        val parts = message.split(" ")
        if (parts.size != 2) return "Неверный формат команды. Используйте: near_lesson GROUP_NUMBER"

        val groupNumber = parts[1]
        val nearestLesson = scheduleService.getNearestLesson(groupNumber)

        return nearestLesson?.let {
            """
            Ближайшее занятие для группы $groupNumber:
            Предмет: ${it.subject}
            Преподаватель: ${it.teacher}
            Аудитория: ${it.classroom}
            Время: ${it.startTime} - ${it.endTime}
            """.trimIndent()
        } ?: "Ближайших занятий не найдено"
    }

    private fun sendNotification(chatId: String, message: String) {
        val sendMessage = SendMessage()
        sendMessage.chatId = chatId
        sendMessage.text = message
        sendMessage.replyMarkup = getKeyboard()
        execute(sendMessage)
    }

    private fun getKeyboard(): ReplyKeyboardMarkup {
        val keyboard = ReplyKeyboardMarkup()
        keyboard.keyboard = listOf(
            KeyboardRow().apply {
                add("update")
                add("near_lesson")
                add("tomorrow")
            },
            KeyboardRow().apply {
                add("monday")
                add("tuesday")
                add("wednesday")
            },
            KeyboardRow().apply {
                add("thursday")
                add("friday")
                add("saturday")
            }
        )
        keyboard.resizeKeyboard = true
        keyboard.selective = true
        return keyboard
    }

    private fun processDaySchedule(message: String): String {
        val parts = message.lowercase().split(" ")
        if (parts.size != 3) return "Неверный формат команды. Используйте: DAY WEEK_NUMBER GROUP_NUMBER"

        val dayOfWeek = try {
            DayOfWeek.valueOf(parts[0].uppercase())
        } catch (e: IllegalArgumentException) {
            return "Неверный день недели. Используйте: monday, tuesday, wednesday, thursday, friday, saturday"
        }

        val weekNumber = parts[1].toIntOrNull()
            ?: return "Неверный номер недели. Используйте числа 1 или 2"
        
        if (weekNumber !in 1..2) {
            return "Номер недели должен быть 1 или 2"
        }

        val groupNumber = parts[2]
        var schedule = scheduleService.getDaySchedule(groupNumber, dayOfWeek, weekNumber)

        if (schedule.isEmpty()) {
            try {
                scheduleService.updateScheduleFromApi(groupNumber)
                schedule = scheduleService.getDaySchedule(groupNumber, dayOfWeek, weekNumber)
            } catch (e: Exception) {
                return "Не удалось получить расписание. Попробуйте сначала обновить его командой: update $groupNumber"
            }
        }

        return if (schedule.isNotEmpty()) {
            buildString {
                appendLine("Расписание для группы $groupNumber на ${dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru"))}:")
                schedule.forEach { lesson ->
                    appendLine("""
                        ${lesson.startTime} - ${lesson.endTime}
                        Предмет: ${lesson.subject}
                        Преподаватель: ${lesson.teacher}
                        Аудитория: ${lesson.classroom}
                        ---------------
                    """.trimIndent())
                }
            }
        } else {
            "В этот день занятий нет"
        }
    }

    private fun processTomorrowSchedule(message: String): String {
        val parts = message.split(" ")
        if (parts.size != 2) return "Неверный формат команды. Используйте: tomorrow GROUP_NUMBER"

        val groupNumber = parts[1]
        val schedule = scheduleService.getTomorrowSchedule(groupNumber)

        return if (schedule.isNotEmpty()) {
            buildString {
                appendLine("Расписание на завтра для группы $groupNumber:")
                schedule.forEach { lesson ->
                    appendLine("""
                        ${lesson.startTime} - ${lesson.endTime}
                        Предмет: ${lesson.subject}
                        Преподаватель: ${lesson.teacher}
                        Аудитория: ${lesson.classroom}
                        ---------------
                    """.trimIndent())
                }
            }
        } else {
            "Занятий на завтра не найдено"
        }
    }

    private fun processWeekSchedule(message: String): String {
        val parts = message.split(" ")
        if (parts.size != 3) return "Неверный формат команды. Используйте: all WEEK_NUMBER GROUP_NUMBER"

        val weekNumber = parts[1].toIntOrNull()
            ?: return "Неверный номер недели. Используйте числа 1 или 2"
        
        if (weekNumber !in 1..2) {
            return "Номер недели должен быть 1 или 2"
        }

        val groupNumber = parts[2]
        val schedule = scheduleService.getWeekSchedule(groupNumber, weekNumber)

        return if (schedule.isNotEmpty()) {
            val scheduleByDay = schedule.groupBy { it.dayOfWeek }
            buildString {
                appendLine("Расписание для группы $groupNumber на неделю $weekNumber:")
                DayOfWeek.values()
                    .filter { it != DayOfWeek.SUNDAY }
                    .forEach { day ->
                        appendLine("\n${day.getDisplayName(TextStyle.FULL, Locale("ru"))}:")
                        scheduleByDay[day]?.sortedBy { it.startTime }?.forEach { lesson ->
                            appendLine("""
                                ${lesson.startTime} - ${lesson.endTime}
                                Предмет: ${lesson.subject}
                                Преподаватель: ${lesson.teacher}
                                Аудитория: ${lesson.classroom}
                                ---------------
                            """.trimIndent())
                        } ?: appendLine("Занятий нет")
                    }
            }
        } else {
            "Расписание на неделю $weekNumber не найдено"
        }
    }
}

private data class UserState(
    val groupNumber: String? = null,
    val day: String? = null,
    val waitingForGroup: Boolean = false
) 