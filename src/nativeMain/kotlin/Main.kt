import arrow.continuations.suspendApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

private val PING_DELAY = Duration.parse("2s")
private val SHUTDOWN_DELAY = Duration.parse("2s")
private val TIMEOUT = SHUTDOWN_DELAY + Duration.parse("1s")


@Suppress("LongMethod")
fun main() = suspendApp(timeout = TIMEOUT) {
    try {
        println("App Started!  Waiting until asked to shutdown.")
        while (true) {
            delay(PING_DELAY)
            println("Ping")
        }
    } catch (e: CancellationException) {
        println(e)
        println("Cleaning up App... will take $SHUTDOWN_DELAY...")
        withContext(NonCancellable) { delay(SHUTDOWN_DELAY) }
        println("Done cleaning up. Will release app to exit")
    }
}
