package no.nav.sokos.spk.mottak.util

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.io.StringReader
import java.io.StringWriter
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

object JaxbUtils {
    private val jaxbContext = JAXBContext.newInstance(Oppdrag::class.java)

    fun marshall(element: Any): String {
        val marshaller =
            jaxbContext.createMarshaller().apply {
                setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
                setProperty(Marshaller.JAXB_ENCODING, "UTF-8")
            }
        return StringWriter().use {
            marshaller.marshal(element, it)
            it.toString()
        }
    }

    fun unmarshall(xmlElement: String): Oppdrag {
        return jaxbContext.createUnmarshaller().unmarshal(
            XMLInputFactory.newInstance().createXMLStreamReader(StreamSource(xmlElement.toValidXml())),
            Oppdrag::class.java,
        ).value
    }

    private fun String.toValidXml() =
        this
            .replace("<oppdrag xmlns=", "<ns2:oppdrag xmlns:ns2=", ignoreCase = true)
            .replace("</Oppdrag>", "</ns2:oppdrag>", ignoreCase = true)
            .let { StringReader(it) }
}
