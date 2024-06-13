package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.TestHelper.verifyFilInfo
import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BEHANDLET_JA
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_RET
import no.nav.sokos.spk.mottak.domain.FILTYPE_INNLEST
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.SftpListener
import java.io.IOException
import java.sql.SQLException

internal class WriteToFileServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, SftpListener))

    val ftpService: FtpService by lazy {
        FtpService(SftpConfig(SftpListener.sftpProperties).createSftpConnection())
    }

    val writeToFileService: WriteToFileService by lazy {
        WriteToFileService(Db2Listener.dataSource, ftpService)
    }

    afterEach {
        SftpListener.deleteFile(
            Directories.ANVISNINGSRETUR.value + "/SPK_NAV_*",
        )
    }

    Given("det finnes innTransaksjoner som er ferdig behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/skrivreturfil/inntTransaksjon_ferdig_behandlet.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet(behandlet = BEHANDLET_JA).size shouldBe 9
        When("skriving av returfil starter") {
            writeToFileService.writeReturnFile()
            Then("skal det opprettes en returfil til SPK som lastes opp til Ftp outbound/anvisningsretur") {
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

        When("skriving av returfil starter") {
            val dataSourceMock = mockk<HikariDataSource>()
            every { dataSourceMock.connection } throws SQLException("No database connection!")
            val writeToFileServiceMock = WriteToFileService(dataSource = dataSourceMock, ftpService = ftpService)

            Then("skal det kastes en MottakException med databasefeil") {
                val exception = shouldThrow<MottakException> { writeToFileServiceMock.writeReturnFile() }
                exception.message shouldBe "Feil under skriving returfil til SPK. Feilmelding: No database connection!"
            }
        }
    }

    Given("det fins innTransaksjoner som er ferdig behandlet med ftp er nede") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/skrivreturfil/inntTransaksjon_ferdig_behandlet.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet(behandlet = BEHANDLET_JA).size shouldBe 9

        When("skriv retur filen prosess starter") {
            val ftpServiceMock = mockk<FtpService>()
            every { ftpServiceMock.createFile(any(), any(), any()) } throws IOException("Ftp server can not move file!")
            val writeToFileServiceMock = WriteToFileService(dataSource = Db2Listener.dataSource, ftpService = ftpServiceMock)

            Then("skal det kastet en MottakException med ftp feil") {
                val exception = shouldThrow<MottakException> { writeToFileServiceMock.writeReturnFile() }
                exception.message shouldBe "Feil under skriving returfil til SPK. Feilmelding: Ftp server can not move file!"
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
