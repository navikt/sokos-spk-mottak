package no.nav.sokos.spk.mottak.listener

import com.ibm.msg.client.jakarta.jms.JmsContext
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.mockk.every
import io.mockk.mockk
import jakarta.jms.ConnectionFactory
import jakarta.jms.Queue
import no.nav.sokos.spk.mottak.mq.JmsProducerService
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

    val connectionFactoryMock: ConnectionFactory = mockk<ConnectionFactory>()
    val jmsContext: JmsContext = mockk<JmsContext>()

    override suspend fun beforeTest(testCase: TestCase) {
        server.start()
        connectionFactory = ActiveMQConnectionFactory("vm:localhost?create=false")

        every { connectionFactoryMock.createContext() } returns jmsContext
    }

    override suspend fun afterTest(
        testCase: TestCase,
        result: TestResult,
    ) {
        server.stop()
    }
}

class JmsProducerTestService(
    connectionFactory: ConnectionFactory,
) : JmsProducerService(connectionFactory) {
    override fun send(
        payload: List<String>,
        queue: Queue,
        replyQueue: Queue,
    ) {
        super.send(payload, ActiveMQQueue(queue.queueName), ActiveMQQueue(replyQueue.queueName))
    }
}
