package no.nav.sokos.spk.mottak.security

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Permission validator for fine-grained access control.
 * Validates scopes (OBO tokens) and roles (M2M tokens) for endpoints.
 */
object AuthorizationGuard {
    /**
     * Get NAVident from OBO token, or null if M2M token.
     * Use this when you need to handle both OBO and M2M tokens differently.
     */
    fun ApplicationCall.getNavIdentOrNull(): String? {
        val principal = principal<JWTPrincipal>() ?: return null
        return principal.payload.getClaim(JWT_CLAIM_NAVIDENT)?.asString()
    }

    /**
     * Get calling system name from JWT token (azp_name or client_id).
     * Useful for logging which application is calling the endpoint.
     * Returns "Ukjent" if not found.
     */
    fun ApplicationCall.getCallingSystem(): String {
        val principal = principal<JWTPrincipal>() ?: return "Ukjent"
        val azpName =
            principal.payload.getClaim("azp_name")?.asString()
                ?: principal.payload.getClaim("client_id")?.asString()
        return TokenUtils.extractApplicationName(azpName)
    }

    /**
     * Require a specific scope (OBO token) OR a specific role (M2M token).
     * Returns true if authorized, false (and sends 403) if not.
     */
    suspend fun ApplicationCall.requireScopeOrRole(scopeOrRole: String): Boolean {
        val principal =
            principal<JWTPrincipal>()
                ?: throw IllegalStateException("No principal found - authentication not configured")

        val callingSystem = getCallingSystem()

        // Check OBO token scope
        val scopes =
            principal.payload
                .getClaim("scp")
                ?.asString()
                ?.split(" ") ?: emptyList()
        if (AccessPolicy.hasRequiredScope(scopes, scopeOrRole)) {
            logger.debug { "Authorized: '$callingSystem' with OBO token has required scope '$scopeOrRole'" }
            return true
        }

        // Check M2M token role
        val roles = principal.payload.getClaim("roles")?.asList(String::class.java) ?: emptyList()
        if (AccessPolicy.hasRequiredRole(roles, scopeOrRole)) {
            logger.debug { "Authorized: '$callingSystem' with M2M token has required role '$scopeOrRole'" }
            return true
        }

        // Neither scope nor role found
        logger.warn {
            "Authorization failed: '$callingSystem' missing required scope/role '$scopeOrRole'. " +
                "Found scopes: $scopes, roles: $roles"
        }
        respond(
            HttpStatusCode.Forbidden,
            mapOf(
                "error" to "Forbidden",
                "message" to "Missing required permission: $scopeOrRole",
            ),
        )
        return false
    }

    /**
     * Require a specific scope (OBO token only).
     * Returns true if authorized, false (and sends 403) if not.
     */
    suspend fun ApplicationCall.requireScope(requiredScope: String): Boolean {
        val principal =
            principal<JWTPrincipal>()
                ?: throw IllegalStateException("No principal found - authentication not configured")

        val callingSystem = getCallingSystem()
        val scopes =
            principal.payload
                .getClaim("scp")
                ?.asString()
                ?.split(" ") ?: emptyList()

        if (!AccessPolicy.hasRequiredScope(scopes, requiredScope)) {
            logger.warn {
                "Authorization failed: '$callingSystem' missing required scope '$requiredScope'. Found scopes: $scopes"
            }
            respond(
                HttpStatusCode.Forbidden,
                mapOf(
                    "error" to "Forbidden",
                    "message" to "Missing required scope: $requiredScope",
                ),
            )
            return false
        }

        logger.debug { "Authorized: '$callingSystem' has required scope '$requiredScope'" }
        return true
    }

    /**
     * Require a specific role (M2M token only).
     * Returns true if authorized, false (and sends 403) if not.
     */
    suspend fun ApplicationCall.requireRole(requiredRole: String): Boolean {
        val principal =
            principal<JWTPrincipal>()
                ?: throw IllegalStateException("No principal found - authentication not configured")

        val callingSystem = getCallingSystem()
        val roles = principal.payload.getClaim("roles")?.asList(String::class.java) ?: emptyList()

        if (!AccessPolicy.hasRequiredRole(roles, requiredRole)) {
            logger.warn {
                "Authorization failed: '$callingSystem' missing required role '$requiredRole'. Found roles: $roles"
            }
            respond(
                HttpStatusCode.Forbidden,
                mapOf(
                    "error" to "Forbidden",
                    "message" to "Missing required role: $requiredRole",
                ),
            )
            return false
        }

        logger.debug { "Authorized: '$callingSystem' has required role '$requiredRole'" }
        return true
    }
}
