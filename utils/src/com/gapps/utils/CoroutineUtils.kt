@file:Suppress("unused", "EXPERIMENTAL_FEATURE_WARNING")

package com.gapps.utils

import com.gapps.utils.Constants.ERROR_LOG_FILE
import com.gapps.utils.Constants.SHOW_EXCEPTIONS
import com.gapps.utils.TimeUnit.M
import kotlinx.coroutines.experimental.*
import java.io.File
import kotlin.system.measureTimeMillis

suspend fun <T> List<Deferred<T>>.joinAllA() = map { it.await() }

suspend fun List<Job?>.joinAllL() = forEach { it?.join() }

suspend fun List<Job?>.cancelAndJoinAllL() = forEach { it?.cancelAndJoin() }

suspend fun launchInterval(interval: Long = 1.toMs(M), delayed: Long? = null, block: suspend () -> Unit) = launch {
    delayed?.let { delay(it) }
    while (isActive) {
        catchS(Unit,
                toIgnore = listOf(JobCancellationException::class.java.canonicalName),
                catch = { delay(interval) }) {
            val intervalDiff = interval - measureTimeMillis { block() }
            if (isActive) delay(if (intervalDiff > 0) intervalDiff else interval)
        }
    }
}

fun launchDelayed(length: Long, block: suspend () -> Unit) = launch {
    delay(length)
    if (isActive) block()
}

suspend fun <T : Any?> catchAsync(returnOnCatch: T,
                                  printStackTrace: Boolean = SHOW_EXCEPTIONS,
                                  logStackTrace: Boolean = true,
                                  toIgnore: List<String>? = null,
                                  catch: (suspend () -> Unit)? = null,
                                  finally: (suspend () -> Unit)? = null,
                                  block: suspend () -> T) =
        async { catchS(returnOnCatch, printStackTrace, logStackTrace, toIgnore, catch, finally, block) }.await()

suspend fun <T : Any?> catchS(returnOnCatch: T,
                              printStackTrace: Boolean = SHOW_EXCEPTIONS,
                              logStackTrace: Boolean = true,
                              toIgnore: List<String>? = null,
                              catch: (suspend () -> Unit)? = null,
                              finally: (suspend () -> Unit)? = null,
                              block: suspend () -> T) = try {
    block()
} catch (throwable: Throwable) {
    if (toIgnore?.contains(throwable::class.java.canonicalName) != true) {
        if (printStackTrace) {
            System.err.println("${throwable.javaClass.simpleName}: ${throwable.message}")
            throwable.printStackTrace()
        }
        if (logStackTrace) throwable.logToFile()
        catch?.invoke()
    }
    returnOnCatch
} finally {
    finally?.invoke()
}

fun <T : Any?> catch(returnOnCatch: T,
                     printStackTrace: Boolean = SHOW_EXCEPTIONS,
                     logStackTrace: Boolean = true,
                     catch: (() -> Unit)? = null,
                     finally: (() -> Unit)? = null,
                     block: () -> T) = try {
    block()
} catch (throwable: Throwable) {
    if (printStackTrace) {
        System.err.println("${throwable.javaClass.simpleName}: ${throwable.message}")
        throwable.printStackTrace()
    }
    if (logStackTrace) throwable.logToFile()
    catch?.invoke()
    returnOnCatch
} finally {
    finally?.invoke()
}

fun Throwable.logToFile() = launch {
    File(ERROR_LOG_FILE).also { file ->
        var log = "${formatDate()}: ${javaClass.simpleName}: $message\n"
        log += stackTrace.joinToString("\n")
        log += "\n\n\n"

        if (file.exists()) log += file.readString()
        file.writeString(log)
    }
}