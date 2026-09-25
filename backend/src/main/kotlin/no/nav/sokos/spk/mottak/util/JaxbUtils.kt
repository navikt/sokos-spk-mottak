package no.nav.sokos.spk.mottak.util

import java.io.StringReader
import java.io.StringWriter
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import no.trygdeetaten.skjema.oppdrag.Oppdrag

import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.ObjectFactory

object JaxbUtils {
    private val jaxbContextOppdrag = JAXBContext.newInstance(Oppdrag::class.java)
    private val jaxbContextAvstemming = JAXBContext.newInstance(Avstemmingsdata::class.java)

    fun marshallOppdrag(oppdrag: Any): String {
        val marshaller =
            jaxbContextOppdrag.createMarshaller().apply {
                setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
                setProperty(Marshaller.JAXB_ENCODING, "UTF-8")
            }
        return StringWriter().use {
            marshaller.marshal(oppdrag, it)
            it.toString()
        }
    }

    fun unmarshallOppdrag(oppdragXml: String): Oppdrag =
        jaxbContextOppdrag
            .createUnmarshaller()
            .unmarshal(
                XMLInputFactory.newInstance().createXMLStreamReader(StreamSource(oppdragXml.toValidOppdragXml())),
                Oppdrag::class.java,
            ).value

    fun marshallAvstemmingsdata(avstemmingsdata: Avstemmingsdata): String {
        val marshaller =
            jaxbContextAvstemming.createMarshaller().apply {
                setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
                setProperty(Marshaller.JAXB_ENCODING, "UTF-8")
            }
        return StringWriter().use {
            marshaller.marshal(ObjectFactory().createAvstemmingsdata(avstemmingsdata), it)
            it.toString()
        }
    }

    private fun String.toValidOppdragXml() =
        this
            .replace("<oppdrag xmlns=", "<ns2:oppdrag xmlns:ns2=", ignoreCase = true)
            .replace("</Oppdrag>", "</ns2:oppdrag>", ignoreCase = true)
            .let { StringReader(it) }
}
