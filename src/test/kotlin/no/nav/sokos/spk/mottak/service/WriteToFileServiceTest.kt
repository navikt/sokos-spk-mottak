package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.TestHelper.verifyFilInfo
import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BEHANDLET_JA
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_RET
import no.nav.sokos.spk.mottak.domain.FILTYPE_INNLEST
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.SftpListener

class WriteToFileServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, SftpListener))

    val ftpService: FtpService by lazy {
        FtpService(SftpConfig(SftpListener.sftpConfig).createSftpConnection())
    }

    val writeToFileService: WriteToFileService by lazy {
        WriteToFileService(Db2Listener.dataSource, ftpService)
    }

    afterEach {
        SftpListener.deleteFile(
            Directories.ANVISNINGSRETUR.value + "/SPK_NAV_*",
        )
    }

    Given("validering innTransaksjon er fullført") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/skrivreturfil/inntTransaksjon_ferdig_behandlet.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet(behandlet = BEHANDLET_JA).size shouldBe 9
        When("det fins innTransaksjon som er ferdig behandlet") {
            writeToFileService.writeReturnFile()
            Then("skal det opprettes retur filen til SPK og laste opp til Ftp outbound/anvisningsretur") {
                Db2Listener.innTransaksjonRepository.getByBehandlet().shouldBeEmpty()
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(34, FILTILSTANDTYPE_RET)!!
                verifyFilInfo(
                    filInfo = filInfo,
                    filStatus = FilStatus.OK,
                    filTilstandType = FILTILSTANDTYPE_RET,
                    fileType = FILTYPE_INNLEST,
                )
                val downloadFile = ftpService.downloadFiles(Directories.ANVISNINGSRETUR)
                downloadFile.size shouldBe 1
                downloadFile.forEach { (filename, content) ->
                    filename shouldBe filInfo.filNavn
                    content.convertArrayListToString() shouldBe readFromResource("/spk/SPK_NAV_RETURFIL.txt")
                }
            }
        }
    }
})

private fun List<String>.convertArrayListToString(): String {
    val stringBuilder = StringBuilder()
    this.forEach { stringBuilder.append(it).appendLine() }
    stringBuilder.setLength(stringBuilder.length - 1)
    return stringBuilder.toString()
}
