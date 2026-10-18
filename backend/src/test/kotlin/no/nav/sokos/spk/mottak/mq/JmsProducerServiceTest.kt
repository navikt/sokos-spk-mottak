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

import no.nav.sokos.spk.mottak.metrics.Metrics

internal class JmsProducerServiceTest :
    FunSpec({
        test("close closes the producer JMSContext") {
            val context = mockk<JMSContext>(relaxed = true)
            val connectionFactory = mockk<ConnectionFactory>()
            every { connectionFactory.createContext() } returns context

            val service =
                JmsProducerService(
                    senderQueue = mockk<Queue>(),
                    metricCounter = Metrics.mqTrekkBOQListenerMetricCounter,
                    connectionFactory = connectionFactory,
                )

            service.close()

            verify(exactly = 1) { context.close() }
        }

        test("producer creation failure closes its JMSContext") {
            val context = mockk<JMSContext>(relaxed = true)
            val connectionFactory = mockk<ConnectionFactory>()
            every { connectionFactory.createContext() } returns context
            every { context.createProducer() } throws JMSException("producer unavailable")

            shouldThrow<JMSException> {
                JmsProducerService(
                    senderQueue = mockk<Queue>(),
                    metricCounter = Metrics.mqTrekkBOQListenerMetricCounter,
                    connectionFactory = connectionFactory,
                )
            }

            verify(exactly = 1) { context.close() }
        }
    })
