package no.nav.sokos.spk.mottak.security

/**
 * Utility functions for JWT token handling.
 */
object TokenUtils {
    /**
     * Extract application name from azp_name or client_id claim.
     * Strips namespace/cluster prefix (e.g., "dev-gcp:okonomi:sokos-utbetalingsportalen" -> "sokos-utbetalingsportalen").
     *
     * @param azpNameOrClientId The azp_name or client_id claim value
     * @return The application name, or "Ukjent" if null/empty
     */
    fun extractApplicationName(azpNameOrClientId: String?): String {
        if (azpNameOrClientId.isNullOrBlank()) return "Ukjent"
        // Strip namespace/cluster prefix by taking last part after ':'
        return azpNameOrClientId.split(":").last()
    }
}
