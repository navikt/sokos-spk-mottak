package no.nav.sokos.spk.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.service.AvstemmingService
import no.nav.sokos.spk.mottak.service.ReadAndParseFileService
import no.nav.sokos.spk.mottak.service.SendTrekkTransaksjonToOppdragZService
import no.nav.sokos.spk.mottak.service.SendUtbetalingTransaksjonToOppdragZService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService
import no.nav.sokos.spk.mottak.service.WriteToFileService

fun Route.mottakApi(
    readAndParseFileService: ReadAndParseFileService = ReadAndParseFileService(),
    validateTransaksjonService: ValidateTransaksjonService = ValidateTransaksjonService(),
    writeToFileService: WriteToFileService = WriteToFileService(),
    sendUtbetalingTransaksjonToOppdragZService: SendUtbetalingTransaksjonToOppdragZService = SendUtbetalingTransaksjonToOppdragZService(),
    sendTrekkTransaksjonToOppdragZService: SendTrekkTransaksjonToOppdragZService = SendTrekkTransaksjonToOppdragZService(),
    avstemmingService: AvstemmingService = AvstemmingService(),
) {
    route("api/v1") {
        get("readParseFileAndValidateTransactions") {
            launch(Dispatchers.IO) {
                readAndParseFileService.readAndParseFile()
                validateTransaksjonService.validateInnTransaksjon()
                writeToFileService.writeReturnFile()
            }
            call.respond(HttpStatusCode.OK, "ReadAndParseFile av filer har startet, sjekk logger for status")
        }

        get("sendUtbetalingTransaksjonToOppdragZ") {
            launch(Dispatchers.IO) {
                sendUtbetalingTransaksjonToOppdragZService.getUtbetalingTransaksjonAndSendToOppdragZ(PropertiesConfig.MQProperties().mqBatchSize)
            }
            call.respond(HttpStatusCode.OK, "SendUtbetalingTransaksjonTilOppdrag har startet, sjekk logger for status")
        }

        get("sendTrekkTransaksjonToOppdragZ") {
            launch(Dispatchers.IO) {
                sendTrekkTransaksjonToOppdragZService.getTrekkTransaksjonAndSendToOppdrag(PropertiesConfig.MQProperties().mqBatchSize)
            }
            call.respond(HttpStatusCode.OK, "SendTrekkTransaksjonTilOppdrag har startet, sjekk logger for status")
        }

        get("avstemming") {
            launch(Dispatchers.IO) {
                avstemmingService.sendGrensesnittAvstemming()
            }
            call.respond(HttpStatusCode.OK, "GrensesnittAvstemming har startet, sjekk logger for status")
        }
    }
}
