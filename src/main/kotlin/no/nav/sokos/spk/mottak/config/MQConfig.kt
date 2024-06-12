package no.nav.sokos.spk.mottak.config

import com.ibm.mq.constants.CMQC.MQENC_NATIVE
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.jms.JmsFactoryFactory
import com.ibm.msg.client.wmq.common.CommonConstants
import javax.jms.ConnectionFactory

private const val UTF_8_WITH_PUA = 1208

object MQConfig {
    fun connectionFactory(properties: PropertiesConfig.MQProperties = PropertiesConfig.MQProperties()): ConnectionFactory {
        return JmsFactoryFactory.getInstance(CommonConstants.WMQ_PROVIDER).createConnectionFactory().apply {
            setIntProperty(CommonConstants.WMQ_CONNECTION_MODE, CommonConstants.WMQ_CM_CLIENT)
            setStringProperty(CommonConstants.WMQ_QUEUE_MANAGER, properties.mqQueueManagerName)
            setStringProperty(CommonConstants.WMQ_HOST_NAME, properties.hostname)
            setStringProperty(CommonConstants.WMQ_APPLICATIONNAME, PropertiesConfig.Configuration().naisAppName)
            setIntProperty(CommonConstants.WMQ_PORT, properties.port)
            setStringProperty(CommonConstants.WMQ_CHANNEL, properties.mqChannelName)
            setIntProperty(CommonConstants.WMQ_CCSID, UTF_8_WITH_PUA)
            setIntProperty(JmsConstants.JMS_IBM_ENCODING, MQENC_NATIVE)
            setIntProperty(JmsConstants.JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA)
            setBooleanProperty(CommonConstants.USER_AUTHENTICATION_MQCSP, true)
            setStringProperty(CommonConstants.USERID, properties.serviceUsername)
            setStringProperty(CommonConstants.PASSWORD, properties.servicePassword)
        }
    }
}
