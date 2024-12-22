import org.jvnet.hk2.annotations.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

@Service
class ScheduleApiService(
    private val webClient: WebClient
) {
    private val apiUrl = "https://your-api-url.com/schedule" // Замените на ваш URL

    fun getSchedule(date: LocalDate): List<Schedule> {
        return webClient.get()
            .uri("$apiUrl?date=${date}")
            .retrieve()
            .bodyToFlux(Schedule::class.java)
            .collectList()
            .block() ?: emptyList()
    }
} 