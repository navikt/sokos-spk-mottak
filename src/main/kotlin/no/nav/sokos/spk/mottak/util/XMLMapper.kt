package no.nav.sokos.spk.mottak.util

import com.ctc.wstx.api.WstxOutputProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

val xmlMapper: XmlMapper =
    XmlMapper(
        JacksonXmlModule().apply { setDefaultUseWrapper(false) },
    ).apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        enable(SerializationFeature.WRAP_ROOT_VALUE)
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
        factory.xmlOutputFactory.setProperty(WstxOutputProperties.P_USE_DOUBLE_QUOTES_IN_XML_DECL, true)
        registerKotlinModule()
        registerModule(JavaTimeModule())
    }
