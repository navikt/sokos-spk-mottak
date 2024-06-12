package no.nav.sokos.spk.mottak.util

import kotliquery.Row
import kotlin.reflect.full.memberProperties

object SQLUtils {
    inline fun <reified T : Any> Row.optionalOrNull(columnLabel: String): T? {
        return runCatching {
            this.any(columnLabel) as? T
        }.getOrNull()
    }

    inline fun <reified T : Any> T.asMap(): Map<String, Any?> {
        val props = T::class.memberProperties.associateBy { it.name }
        return props.keys.associateWith { props[it]?.get(this) }
    }
}
