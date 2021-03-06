package support.bymason.kiosk.checkin.feature.hostfinder

import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import support.bymason.kiosk.checkin.core.data.Cache
import support.bymason.kiosk.checkin.core.data.CheckInApi
import support.bymason.kiosk.checkin.core.data.DispatcherProvider
import support.bymason.kiosk.checkin.core.data.FreshCache
import support.bymason.kiosk.checkin.core.model.Host

interface HostRepository {
    suspend fun find(name: String): List<Host>

    suspend fun registerHost(sessionId: String, host: Host): String
}

class DefaultHostRepository(
        dispatchers: DispatcherProvider,
        private val api: CheckInApi,
        private val cache: Cache = FreshCache(dispatchers)
) : HostRepository {
    private val json = Json(JsonConfiguration.Stable)

    override suspend fun find(name: String): List<Host> {
        val input = Cache.Input(
                keys = *arrayOf("findHosts", name),
                processedToRaw = { json.stringify(Host.serializer().list, it) },
                rawToProcessed = { json.parse(Host.serializer().list, it) }
        )
        return cache.memoize(input) {
            api.findHosts(name)
        }
    }

    override suspend fun registerHost(
            sessionId: String,
            host: Host
    ): String {
        return api.updateSessionForHereToSee(sessionId, host.id)
    }
}
