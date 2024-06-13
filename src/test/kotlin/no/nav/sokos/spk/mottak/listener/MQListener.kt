package no.nav.sokos.spk.mottak.listener

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import io.kotest.core.listeners.TestListener
import org.testcontainers.containers.GenericContainer

object MQListener : TestListener {
    private val container =
        GenericContainer("ibmcom/mq")
            .withExposedPorts(1413)
            .withEnv("LICENSE", "accept")
            .withEnv("MQ_QMGR_NAME", "QM1")
            .withEnv("MQ_USER_NAME", "admin")
            .withCommand("--volume q1data:/mnt/mqm")
            .withCreateContainerCmdModifier { cmd ->
                cmd.hostConfig!!.withPortBindings(PortBinding(Ports.Binding.bindPort(1413), ExposedPort(1413)))
            }
}
