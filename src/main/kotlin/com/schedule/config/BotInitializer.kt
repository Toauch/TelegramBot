package com.schedule.config

import com.schedule.bot.ScheduleBot
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Component
class BotInitializer(
    private val scheduleBot: ScheduleBot
) {
    @EventListener(ContextRefreshedEvent::class)
    fun init() {
        val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
        try {
            telegramBotsApi.registerBot(scheduleBot)
        } catch (e: Exception) {
            throw RuntimeException("Ошибка при инициализации бота", e)
        }
    }
} 