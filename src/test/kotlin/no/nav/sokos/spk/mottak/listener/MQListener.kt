package no.nav.sokos.spk.mottak.listener

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.jms.JmsConstants
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.mockk.mockk
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.JMSProducer
import no.nav.sokos.spk.mottak.listener.MQListener.testContext
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
    lateinit var testContext: JMSContext
    val senderQueueMock = mockk<MQQueue>()
    val replyQueueMock = mockk<MQQueue>()

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

class JmsProducerServiceTestService(
    private val senderQueue: MQQueue,
    private val replyQueue: MQQueue,
    private val conFactory: ConnectionFactory,
) : JmsProducerService(senderQueue, replyQueue, conFactory) {
    override fun send(payload: List<String>) {
        val senderActiveMQQueue = ActiveMQQueue(senderQueue.queueName)
        val replyActiveMQQueue = ActiveMQQueue(replyQueue.queueName)
        val producer: JMSProducer = testContext.createProducer().apply { jmsReplyTo = replyActiveMQQueue }
        testContext.createContext(JmsConstants.SESSION_TRANSACTED).use { ctx ->
            val messages = payload.map { ctx.createTextMessage(it) }
            runCatching {
                messages.forEach { message ->
                    producer.send(senderActiveMQQueue, message)
                }
            }.onSuccess {
                ctx.commit()
            }.onFailure { exception ->
                ctx.rollback()
            }
        }
    }
}
