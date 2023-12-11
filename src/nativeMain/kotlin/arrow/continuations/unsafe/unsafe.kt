package arrow.continuations.unsafe

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.posix.SIGINT
import platform.posix.SIGTERM
import platform.posix.signal
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

private val SIGNAL: CompletableDeferred<Int> = CompletableDeferred()

private val BACKPRESSURE: CompletableDeferred<Int> = CompletableDeferred()

@OptIn(ExperimentalForeignApi::class)
private val SignalHandler =
    staticCFunction<Int, Unit> { code ->
        println("SignalHandler: signal code = $code")
        SIGNAL.complete(code)
        println("SignalHandler: BACKPRESSURE.await()")
        val finalCode: Int = runBlocking { BACKPRESSURE.await() }
        println("SignalHandler: exitProcess($finalCode)")
        exitProcess(finalCode)
    }

object Unsafe {
    @OptIn(DelicateCoroutinesApi::class, ExperimentalForeignApi::class)
    fun onShutdown(block: suspend () -> Unit): () -> Unit {
        println("Unsafe.onShutdown: GlobalScope.launch")
        GlobalScope.launch {
            println("Unsafe.onShutdown: GlobalScope.launch: SIGNAL.await()")
            val code: Int = SIGNAL.await()
            println("Unsafe.onShutdown: GlobalScope.launch: SIGNAL.await(): code = $code")
            println("Unsafe.onShutdown: GlobalScope.launch: runCatching { block() }")
            val res: Result<Unit> = runCatching { block() }
            println("Unsafe.onShutdown: GlobalScope.launch: runCatching { block() }: res = $res")
            println("Unsafe.onShutdown: GlobalScope.launch: BACKPRESSURE.complete")
            BACKPRESSURE.complete(res.fold({ code }, { -1 }))
            print("Unsafe.onShutdown: GlobalScope.launch: BACKPRESSURE completed")
        }

        println("Unsafe.onShutdown: signal(SIGTERM, SignalHandler)")
        signal(SIGTERM, SignalHandler)
        println("Unsafe.onShutdown: signal(SIGINT, SignalHandler)")
        signal(SIGINT, SignalHandler)

        println("Unsafe.onShutdown: return {}")
        return {}
    }

    fun runCoroutineScope(
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> Unit,
    ): Unit = runBlocking(context, block)
}
