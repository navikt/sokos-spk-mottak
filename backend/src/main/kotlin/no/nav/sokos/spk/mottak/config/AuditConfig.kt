package no.nav.sokos.spk.mottak.config

import mu.KotlinLogging

const val AUDIT_LOGGER = "auditLogger"

private const val VERSION = "0"
private const val DEVICE_VENDOR = "SPK Mottak Dashboard"
private const val DEVICE_PRODUCT = "sokos-spk-mottak"
private const val DEVICE_VERSION = "1.0"
private const val DEVICE_EVENT_CLASS_ID = "audit:access"
private const val NAME = "sokos-spk-mottak"
private const val SEVERITY = "INFO"

data class AuditLogg(
    val navIdent: String,
    val jobName: String,
    val brukerBehandlingTekst: String,
) {
    fun logMessage(): String {
        val extension = "suid=$navIdent duid=$jobName end=${System.currentTimeMillis()} msg=$brukerBehandlingTekst"

        return "CEF:$VERSION|$DEVICE_VENDOR|$DEVICE_PRODUCT|$DEVICE_VERSION|$DEVICE_EVENT_CLASS_ID|$NAME|$SEVERITY|$extension"
    }
}

private val auditLogger = KotlinLogging.logger(AUDIT_LOGGER)

class AuditLogger {
    fun auditLog(auditLoggData: AuditLogg) {
        auditLogger.info(auditLoggData.logMessage())
    }
}
