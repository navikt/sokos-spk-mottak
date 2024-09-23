package no.nav.sokos.spk.mottak.util

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.management.modelmbean.XMLParseException
import javax.xml.parsers.DocumentBuilderFactory

object XmlUtils {
    fun parseXmlToStringList(
        xml: String,
        tagName: String,
    ): List<String> {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8))
        val doc = dBuilder.parse(xmlInput)
        val list = mutableListOf<String>()

        val nodes = doc.getElementsByTagName(tagName)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                list.add(element.textContent)
            }
        }
        if (list.isEmpty()) {
            throw XMLParseException("Could not find tag $tagName in xml")
        }
        return list
    }
}
