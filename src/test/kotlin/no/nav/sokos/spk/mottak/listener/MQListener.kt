package no.nav.sokos.spk.mottak.listener

import com.ibm.mq.jakarta.jms.MQQueue
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import jakarta.jms.Connection
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.JMSContext.AUTO_ACKNOWLEDGE
import jakarta.jms.JMSContext.SESSION_TRANSACTED
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.mq.MQ
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
    private lateinit var connection: Connection

    override suspend fun beforeTest(testCase: TestCase) {
        server.start()
        connectionFactory = ActiveMQConnectionFactory("vm://localhost?create=false")
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
    private val jmsContext: JMSContext = connectionFactory.createContext()

    override fun send(
        payload: String,
        queueName: String,
        replyQueueName: String,
    ) {
        jmsContext.createContext(AUTO_ACKNOWLEDGE).use { context ->
            val destination = context.createQueue(MQQueue(queueName).queueURI)
            val message =
                context.createTextMessage(payload).apply {
                    jmsReplyTo = ActiveMQQueue(replyQueueName)
                }
            context.createProducer().send(destination, message)
        }
    }
}

class MQTest(
    senderQueue: MQQueue,
    replyQueue: MQQueue,
) : MQ(senderQueue, replyQueue, connectionFactory.createContext(SESSION_TRANSACTED)) {
    val ctx: JMSContext = connectionFactory.createContext(SESSION_TRANSACTED)
    val destination = context.createQueue(MQQueue(senderQueue.queueName).queueURI)
    val reply = context.createQueue(MQQueue(replyQueue.queueName).queueURI)

    override fun send(message: String) {
        val producer = context.createProducer().apply { jmsReplyTo = reply }
        ctx.use { ctx ->
            runCatching {
                producer.send(destination, message)
            }.onSuccess {
                ctx.commit()
            }.onFailure {
                ctx.rollback()
            }
        }
    }
}
