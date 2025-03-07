package no.nav.sokos.spk.mottak.service

import java.io.IOException
import java.sql.SQLException
import java.time.LocalDate

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.KotestInternal
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.time.withConstantNow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import kotliquery.queryOf

import no.nav.sokos.spk.mottak.TestHelper.convertArrayListToString
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.TestHelper.verifyFilInfo
import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_RET
import no.nav.sokos.spk.mottak.domain.FILTYPE_AVREGNING
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.SftpListener
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

@OptIn(KotestInternal::class)
internal class WriteAvregningsreturFileServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, SftpListener))

    val ftpService: FtpService by lazy {
        FtpService(SftpConfig(SftpListener.sftpProperties))
    }

    val writeAvregningsreturFileService: WriteAvregningsreturFileService by lazy {
        WriteAvregningsreturFileService(
            dataSource = Db2Listener.dataSource,
            ftpService = ftpService,
        )
    }

    afterEach {
        SftpListener.deleteFile(
            Directories.AVREGNINGSRETUR.value + "/SPK_NAV_*",
        )
    }

    Given("det finnes avregningsretur transaksjoner som ikke er sent til FTP") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/skrivreturfil/avregningsretur_ikke_sendt.sql")))
        }
        Db2Listener.avregningsreturRepository.getReturTilAnviserWhichIsNotSent().size shouldBe 32
        When("skriving av returfiler starter") {
            withConstantNow(LocalDate.of(2025, 1, 1)) {
                writeAvregningsreturFileService.writeAvregningsreturFile()
            }
            Then("skal det opprettes avregningsreturfilen til SPK som lastes opp til Ftp outbound/avregning") {
                Db2Listener.avregningsreturRepository.getReturTilAnviserWhichIsNotSent().shouldBeEmpty()
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_RET, listOf("000011")).first()
                verifyFilInfo(
                    filInfo = filInfo,
                    filStatus = FilStatus.OK,
                    filTilstandType = FILTILSTANDTYPE_RET,
                    fileType = FILTYPE_AVREGNING,
                )

                val downloadFile = ftpService.downloadFiles(Directories.AVREGNINGSRETUR)
                downloadFile.forEach { (filename, content) ->
                    filename shouldStartWith "SPK_NAV_"
                    filename shouldEndWith "_AVR"
                    content.convertArrayListToString() shouldBe readFromResource("/spk/SPK_NAV_AVREGNINGSRETUR.txt")
                }
            }
        }

        When("skriving av returfil starter") {
            val dataSourceMock = mockk<HikariDataSource>()
            every { dataSourceMock.connection } throws SQLException("No database connection!")
            val writeAvregningsreturFileServiceMock = WriteAvregningsreturFileService(dataSource = dataSourceMock, ftpService = ftpService)

            Then("skal det kastes en MottakException med databasefeil") {
                val exception = shouldThrow<MottakException> { writeAvregningsreturFileServiceMock.writeAvregningsreturFile() }
                exception.message shouldBe "Skriving av avregningfil feilet. Feilmelding: No database connection!"
            }
        }
    }

    Given("det fins avregningsretur som ikke sent til FTP med FTP server er nede") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/skrivreturfil/avregningsretur_ikke_sendt.sql")))
        }
        Db2Listener.avregningsreturRepository.getReturTilAnviserWhichIsNotSent().size shouldBe 32

        When("skriv retur filen prosess starter") {
            val ftpServiceMock = mockk<FtpService>()
            every { ftpServiceMock.createFile(any(), any(), any()) } throws IOException("Ftp server can not move file!")
            val writeAvregningsreturFileServiceMock = WriteAvregningsreturFileService(dataSource = Db2Listener.dataSource, ftpService = ftpServiceMock)

            Then("skal det kastet en MottakException med ftp feil") {
                val exception = shouldThrow<MottakException> { writeAvregningsreturFileServiceMock.writeAvregningsreturFile() }
                exception.message shouldBe "Skriving av avregningfil feilet. Feilmelding: Ftp server can not move file!"

                Db2Listener.avregningsreturRepository.getReturTilAnviserWhichIsNotSent().size shouldBe 32
                Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_RET, listOf("000011")).shouldBeEmpty()
            }
        }
    }
})
