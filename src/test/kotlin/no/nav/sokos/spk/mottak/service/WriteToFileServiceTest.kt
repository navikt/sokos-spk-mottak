package no.nav.sokos.spk.mottak.service

import java.io.IOException
import java.sql.SQLException

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kotliquery.queryOf

import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.TestHelper.verifyFilInfo
import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.domain.BEHANDLET_JA
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_RET
import no.nav.sokos.spk.mottak.domain.FILTYPE_INNLEST
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.SftpListener
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

internal class WriteToFileServiceTest :
    BehaviorSpec({
        extensions(listOf(Db2Listener, SftpListener))

        val ftpService: FtpService by lazy {
            FtpService(SftpConfig(SftpListener.sftpProperties))
        }

        val writeToFileService: WriteToFileService by lazy {
            WriteToFileService(
                dataSource = Db2Listener.dataSource,
                ftpService = ftpService,
            )
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
            Db2Listener.innTransaksjonRepository.getByBehandlet(behandlet = BEHANDLET_JA).size shouldBe 11
            When("skriving av returfiler starter") {
                writeToFileService.writeReturnFile()
                Then("skal det opprettes to returfiler til SPK som lastes opp til Ftp outbound/anvisningsretur") {
                    Db2Listener.innTransaksjonRepository.getByBehandlet().shouldBeEmpty()
                    val filInfo1 = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_RET, listOf("000034")).first()
                    verifyFilInfo(
                        filInfo = filInfo1,
                        filStatus = FilStatus.OK,
                        filTilstandType = FILTILSTANDTYPE_RET,
                        fileType = FILTYPE_INNLEST,
                    )
                    val filInfo2 = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_RET, listOf("000035")).first()
                    verifyFilInfo(
                        filInfo = filInfo2,
                        filStatus = FilStatus.OK,
                        filTilstandType = FILTILSTANDTYPE_RET,
                        fileType = FILTYPE_INNLEST,
                    )
                    filInfo1.filNavn shouldNotBe filInfo2.filNavn
                    val downloadFile = ftpService.downloadFiles(Directories.ANVISNINGSRETUR)
                    downloadFile.size shouldBe 2
                    downloadFile.forEach { (filename, content) ->
                        if (filename.equals(filInfo1.filNavn)) {
                            content.convertArrayListToString() shouldBe readFromResource("/spk/SPK_NAV_RETURFIL1.txt")
                        } else if (filename.equals(filInfo2.filNavn)) {
                            content.convertArrayListToString() shouldBe readFromResource("/spk/SPK_NAV_RETURFIL2.txt")
                        } else {
                            throw Exception("Unexpected file name: $filename")
                        }
                    }
                }
            }

            When("skriving av returfil starter") {
                val dataSourceMock = mockk<HikariDataSource>()
                every { dataSourceMock.connection } throws SQLException("No database connection!")
                val writeToFileServiceMock = WriteToFileService(dataSource = dataSourceMock, ftpService = ftpService)

                Then("skal det kastes en MottakException med databasefeil") {
                    val exception = shouldThrow<MottakException> { writeToFileServiceMock.writeReturnFile() }
                    exception.message shouldBe "Skriving av returfil feilet. Feilmelding: No database connection!"
                }
            }
        }

        Given("det fins innTransaksjoner som er ferdig behandlet med ftp er nede") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/skrivreturfil/inntTransaksjon_ferdig_behandlet.sql")))
            }
            Db2Listener.innTransaksjonRepository.getByBehandlet(behandlet = BEHANDLET_JA).size shouldBe 11

            When("skriv retur filen prosess starter") {
                val ftpServiceMock = mockk<FtpService>()
                every { ftpServiceMock.createFile(any(), any(), any()) } throws IOException("Ftp server can not move file!")
                val writeToFileServiceMock = WriteToFileService(dataSource = Db2Listener.dataSource, ftpService = ftpServiceMock)

                Then("skal det kastet en MottakException med ftp feil") {
                    val exception = shouldThrow<MottakException> { writeToFileServiceMock.writeReturnFile() }
                    exception.message shouldBe "Skriving av returfil feilet. Feilmelding: Ftp server can not move file!"
                }
            }
        }
    })

private fun List<String>.convertArrayListToString(): String {
    val stringBuilder = StringBuilder()
    this.forEach { stringBuilder.append(it.trim()).appendLine() }
    stringBuilder.setLength(stringBuilder.length - 1)
    return stringBuilder.toString()
}
