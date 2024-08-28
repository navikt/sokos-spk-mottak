package no.nav.sokos.spk.mottak.listener

import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.mockk.mockk
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import org.apache.activemq.artemis.api.core.TransportConfiguration
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory
import org.apache.activemq.artemis.jms.client.ActiveMQQueue

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
    lateinit var testContext: JMSContext
    val senderQueueMock = mockk<ActiveMQQueue>()
    val replyQueueMock = mockk<ActiveMQQueue>()

    override suspend fun beforeTest(testCase: TestCase) {
        server.start()
        connectionFactory = ActiveMQConnectionFactory("vm:localhost?create=false")
        testContext = connectionFactory.createContext()
    }

    override suspend fun afterTest(
        testCase: TestCase,
        result: TestResult,
    ) {
        server.stop()
    }
}
