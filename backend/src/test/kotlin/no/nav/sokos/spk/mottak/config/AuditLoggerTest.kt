package no.nav.sokos.spk.mottak.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith

internal class AuditLoggerTest :
    FunSpec({

        test("test auditLogger har riktig melding format") {
            val expectedLogMessageStart =
                "CEF:0|SPK Mottak Dashboard|sokos-spk-mottak|1.0|audit:access|sokos-spk-mottak|INFO|suid=Z12345 duid=avstemming end="
            val expectedLogMessageEnd = " msg=Dette er en brukerbehandlingstekst"
            val logData =
                AuditLogg(
                    navIdent = "Z12345",
                    jobName = "avstemming",
                    brukerBehandlingTekst = "Dette er en brukerbehandlingstekst",
                )

            println(logData.logMessage())

            logData.logMessage() shouldStartWith expectedLogMessageStart
            logData.logMessage() shouldEndWith expectedLogMessageEnd
        }
    })
