package no.nav.sokos.spk.mottak.util

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.management.modelmbean.XMLParseException
import javax.xml.parsers.DocumentBuilderFactory

object XmlUtils {
    private fun parseXml(xml: String): Document {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8))
        return dBuilder.parse(xmlInput)
    }

    fun parseXmlToStringList(
        xml: String,
        tagName: String,
    ): List<String> {
        val doc = parseXml(xml)
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

    fun parseXmlToString(
        xml: String,
        tagName: String,
    ): String {
        val doc = parseXml(xml)

        val nodes = doc.getElementsByTagName(tagName)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                return element.textContent
            }
        }
        throw XMLParseException("Could not find tag $tagName in xml")
    }
}
