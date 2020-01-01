package com.gitlab.kordlib.rest.ratelimit

import com.gitlab.kordlib.common.annotation.KordUnsafe
import com.gitlab.kordlib.common.ratelimit.BucketRateLimiter
import com.gitlab.kordlib.rest.request.Request
import com.gitlab.kordlib.rest.request.RequestException
import com.gitlab.kordlib.rest.request.RequestIdentifier
import com.gitlab.kordlib.rest.route.Route
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.call.receive
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.takeFrom
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.time.minutes

private val logger = KotlinLogging.logger {}

@KordUnsafe
class ParallelRequestHandler(private val client: HttpClient, private val clock: Clock = Clock.systemUTC()) : RequestHandler {

    private var globalSuspensionPoint = atomic(0L)

    private val routeSuspensionPoints = atomic(mutableMapOf<RequestIdentifier, Long>())

    private val autoBanRateLimiter = BucketRateLimiter(25000, 10.minutes)

    private val locks: ConcurrentMap<RequestIdentifier, Mutex> = ConcurrentHashMap()

    override tailrec suspend fun <T> handle(request: Request<T>): HttpResponse {
        val builder = HttpRequestBuilder().apply {
            headers.append("X-RateLimit-Precision", "millisecond")
            url.takeFrom(Route.baseUrl)
            with(request) { apply() }
        }

        logger.trace { "REQUEST: ${request.logString}" }
        val identifier = request.identifier
        val mutex = locks.getOrPut(identifier) { Mutex() }
        mutex.withLock {
            suspendFor(request)

            val response = client.request<HttpStatement>(builder).execute()

            logger.trace { response.logString }

            if (response.isGlobalRateLimit) {
                logger.trace { "GLOBAL RATE LIMIT UNTIL ${response.globalSuspensionPoint(clock)}: ${request.logString}" }
                globalSuspensionPoint.update { response.globalSuspensionPoint(clock) }
            }

            if (response.isChannelRateLimit) {
                logger.trace { "ROUTE RATE LIMIT UNTIL ${response.channelSuspensionPoint}: ${request.logString}" }
                routeSuspensionPoints.value[request.identifier] = response.channelSuspensionPoint
            }

            if (response.isRateLimit) {
                autoBanRateLimiter.consume()
                return handle(request)
            }

            if (response.isErrorWithRateLimit) {
                autoBanRateLimiter.consume()
            }

            if (response.isError) {
                throw RequestException(response, response.errorString())
            }
            return response
        }
    }

    private suspend fun suspendFor(request: Request<*>) {
        delay(globalSuspensionPoint.value - clock.millis())
        globalSuspensionPoint.update { 0 }

        val key = request.identifier
        val routeSuspensionPoint = routeSuspensionPoints.value[key]

        if (routeSuspensionPoint != null) {
            delay(routeSuspensionPoint - clock.millis())
            routeSuspensionPoints.value.remove(key)
        }
    }
}