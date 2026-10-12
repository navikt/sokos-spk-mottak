package no.nav.sokos.spk.mottak.repository

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import no.nav.sokos.spk.mottak.domain.RyddeStopp
import no.nav.sokos.spk.mottak.domain.RyddeStoppgrunn

class TilstandsRyddeLasTest :
    FunSpec({
        test("opptatt lås returnerer ubrukt sesjon uten unlock") {
            val f = LasFikstur()
            every { f.tatt.getBoolean(1) } returns false
            f.repository.provKjoringslas() shouldBe null
            verify(exactly = 1) { f.forbindelse.close() }
            verify(exactly = 0) { f.forbindelse.prepareStatement("SELECT pg_advisory_unlock(?)") }
        }
        test("helse sjekker pg_locks i den låste fysiske sesjonen og ikke SELECT 1") {
            val f = LasFikstur()
            val las = f.repository.provKjoringslas()!!
            las.kontroller(2)
            verify { f.helse.queryTimeout = 2 }
            verify { f.helse.setLong(1, TILSTANDSRYDDING_LASENOKKEL ushr 32) }
            verify { f.helse.setLong(2, TILSTANDSRYDDING_LASENOKKEL and 0xffffffffL) }
            every { f.eid.getBoolean(1) } returns false
            shouldThrow<RyddeLasTapt> { las.kontroller() }
            las.close()
            verify(exactly = 1) { f.forbindelse.prepareStatement("SELECT pg_advisory_unlock(?)") }
        }
        test("unlockfeil forkaster den fysiske sesjonen før proxyen returneres") {
            val f = LasFikstur()
            val las = f.repository.provKjoringslas()!!
            every { f.frigjor.executeQuery() } throws SQLException("sensitiv", "08006")
            shouldThrow<SQLException> { las.close() }
            verify(exactly = 1) { f.dataSource.evictConnection(f.forbindelse) }
            las.close()
            verify(exactly = 1) { f.frigjor.executeQuery() }
        }
        test("false fra unlock behandles som tapt lås og sesjonen forkastes") {
            val f = LasFikstur()
            val las = f.repository.provKjoringslas()!!
            every { f.frigjort.getBoolean(1) } returns false
            shouldThrow<RyddeLasTapt> { las.close() }
            verify(exactly = 1) { f.dataSource.evictConnection(f.forbindelse) }
        }
        test("feil ved låseanskaffelse forkaster sesjonen også når serverutfallet er ukjent") {
            val f = LasFikstur()
            every { f.ta.executeQuery() } throws SQLException("ukjent utfall")
            shouldThrow<SQLException> { f.repository.provKjoringslas() }
            verify(exactly = 1) { f.dataSource.evictConnection(f.forbindelse) }
        }
        test("også Postgres-låseanskaffelse bruker gjenstående budsjett som timeout") {
            val f = LasFikstur()
            f.repository.provKjoringslas(RyddeEnhetskontroll { 1 })!!.use { }
            verify { f.ta.queryTimeout = 1 }
        }
        test("poolventing som bruker budsjettet gir ingen SQL og forkaster forbindelsen") {
            val f = LasFikstur()
            var utlop = false
            every { f.dataSource.connection } answers {
                utlop = true
                f.forbindelse
            }
            shouldThrow<RyddeStopp> {
                f.repository.provKjoringslas(
                    RyddeEnhetskontroll {
                        if (utlop) throw RyddeStopp(RyddeStoppgrunn.BUDSJETT)
                        1
                    },
                )
            }
            verify(exactly = 0) { f.ta.executeQuery() }
            verify(exactly = 1) { f.dataSource.evictConnection(f.forbindelse) }
        }
        test("utløpt budsjett ved avslutning forkaster låsesesjonen uten ny unlockspørring") {
            val f = LasFikstur()
            var utlop = false
            val las =
                f.repository.provKjoringslas(
                    RyddeEnhetskontroll {
                        if (utlop) throw RyddeStopp(RyddeStoppgrunn.BUDSJETT)
                        1
                    },
                )!!
            utlop = true
            las.close()
            verify(exactly = 0) { f.frigjor.executeQuery() }
            verify(exactly = 1) { f.dataSource.evictConnection(f.forbindelse) }
        }
    })

private class LasFikstur {
    val dataSource = mockk<HikariDataSource>(relaxed = true)
    val forbindelse = mockk<Connection>(relaxed = true)
    val ta = mockk<PreparedStatement>(relaxed = true)
    val helse = mockk<PreparedStatement>(relaxed = true)
    val frigjor = mockk<PreparedStatement>(relaxed = true)
    val tatt = mockk<ResultSet>(relaxed = true)
    val eid = mockk<ResultSet>(relaxed = true)
    val frigjort = mockk<ResultSet>(relaxed = true)

    init {
        every { dataSource.maximumPoolSize } returns 3
        every { dataSource.connection } returns forbindelse
        every { forbindelse.prepareStatement("SELECT pg_try_advisory_lock(?)") } returns ta
        every { forbindelse.prepareStatement(match { it.contains("FROM pg_locks") }) } returns helse
        every { forbindelse.prepareStatement("SELECT pg_advisory_unlock(?)") } returns frigjor
        every { ta.executeQuery() } returns tatt
        every { helse.executeQuery() } returns eid
        every { frigjor.executeQuery() } returns frigjort
        listOf(tatt, eid, frigjort).forEach { resultat ->
            every { resultat.next() } returns true
            every { resultat.getBoolean(1) } returns true
        }
    }

    val repository = TilstandsRyddeStatusRepository(dataSource)
}
