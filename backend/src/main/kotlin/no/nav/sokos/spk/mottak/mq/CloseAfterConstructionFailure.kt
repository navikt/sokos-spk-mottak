package no.nav.sokos.spk.mottak.mq

internal fun AutoCloseable.closeAfterConstructionFailure(exception: Exception) {
    try {
        close()
    } catch (closeException: Exception) {
        exception.addSuppressed(closeException)
    }
}
