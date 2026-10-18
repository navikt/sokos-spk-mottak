package no.nav.sokos.spk.mottak.mq

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.JMSException
import jakarta.jms.Queue

internal class JmsListenerTest :
    FunSpec({
        test("start starter JMSContext og close lukker det") {
            val context = mockk<JMSContext>(relaxed = true)
            val connectionFactory = mockk<ConnectionFactory>()
            every { connectionFactory.createContext(JMSContext.CLIENT_ACKNOWLEDGE) } returns context
            val listener = TestJmsListener(connectionFactory)

            listener.start()
            listener.close()

            verify(exactly = 1) { context.start() }
            verify(exactly = 1) { context.close() }
        }

        test("consumer creation failure closes the context") {
            val context = mockk<JMSContext>(relaxed = true)
            val connectionFactory = mockk<ConnectionFactory>()
            val queue = mockk<Queue>()
            every { connectionFactory.createContext(JMSContext.CLIENT_ACKNOWLEDGE) } returns context
            every { context.createConsumer(queue) } throws JMSException("queue unavailable")

            shouldThrow<JMSException> {
                FailingJmsListener(connectionFactory, queue)
            }

            verify(exactly = 1) { context.close() }
        }
    })

private class TestJmsListener(
    connectionFactory: ConnectionFactory,
) : JmsListener(connectionFactory)

private class FailingJmsListener(
    connectionFactory: ConnectionFactory,
    queue: Queue,
) : JmsListener(connectionFactory) {
    init {
        createConsumer(queue) {}
    }
}
