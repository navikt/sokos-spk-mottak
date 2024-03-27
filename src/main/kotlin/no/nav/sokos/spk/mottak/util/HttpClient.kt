package no.nav.sokos.spk.mottak.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.net.ProxySelector
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.apache.http.impl.conn.SystemDefaultRoutePlanner

val httpClient = HttpClient(Apache) {
    expectSuccess = false

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true

            @OptIn(ExperimentalSerializationApi::class)
            explicitNulls = false
        }
        )
    }

    engine {
        customizeClient {
            setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
        }
    }
}
