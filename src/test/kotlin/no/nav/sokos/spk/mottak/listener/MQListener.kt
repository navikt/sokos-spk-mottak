package no.nav.sokos.spk.mottak.listener

import kotlinx.serialization.json.Json

import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import io.mockk.mockk
import io.prometheus.metrics.core.metrics.Counter
import jakarta.jms.ConnectionFactory
import org.apache.activemq.artemis.api.core.TransportConfiguration
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory
import org.apache.activemq.artemis.jms.client.ActiveMQQueue

import no.nav.sokos.spk.mottak.metrics.Metrics.prometheusMeterRegistry

object MQListener : TestListener {
    private val server =
        EmbeddedActiveMQ()
            .setConfiguration(
                ConfigurationImpl()
                    .setPersistenceEnabled(false)
                    .setSecurityEnabled(false)
                    .addAcceptorConfiguration(TransportConfiguration(InVMAcceptorFactory::class.java.name)),
            )

    lateinit var connectionFactory: ConnectionFactory
    val senderQueueMock = mockk<ActiveMQQueue>()
    val replyQueueMock = mockk<ActiveMQQueue>()

    val tempMetric = Counter.builder().name("temp_metric").withoutExemplars().register(prometheusMeterRegistry.prometheusRegistry)
    val json = Json { ignoreUnknownKeys = true }

    override suspend fun beforeSpec(spec: Spec) {
        server.start()
        connectionFactory = ActiveMQConnectionFactory("vm:localhost?create=false")
    }

    override suspend fun afterSpec(spec: Spec) {
        server.stop()
    }
}
