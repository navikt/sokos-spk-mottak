# Authorization Patterns - Bruksveiledning

Denne appen støtter tre autorisasjonsmønstre for endpoints:

## 📋 Pattern 1: OBO-only (Kun brukerbasert)

**Bruk når:** Endepunktet MÅ ha NAVident for audit trail

**Eksempel:**
```kotlin
post("sendTransaction") {
    if (!call.requireScope("transaction.write")) return@post
    val ident = call.getSaksbehandler() // Safe - requireScope garanterer NAVident
    
    // Business logic med ident for audit logging
    logger.info { "Transaction sent by $ident" }
}
```

**Token-krav:**
- ✅ OBO token med scope `transaction.write`
- ❌ M2M token blir avvist (mangler NAVident)

**NAIS config:**
```yaml
- application: saksbehandling-app
  permissions:
    scopes:
      - "transaction.write"
```

---

## 📋 Pattern 2: Fleksibel (OBO eller M2M, ulik håndtering)

**Bruk når:** Endepunktet kan kalles av både brukere og systemer, men du vil logge ulikt

**Eksempel:**
```kotlin
get("leveattester/{datoFom}") {
    if (!call.requireScopeOrRole("leveattester.read")) return@get
    
    // Hent NAVident hvis OBO, null hvis M2M
    val navIdent = call.getNavIdentOrNull()
    val tokenType = if (call.isOboToken()) "OBO" else "M2M"
    
    logger.info { "Leveattester accessed by $tokenType${navIdent?.let { " (user: $it)" } ?: ""}" }
    
    // Business logic (samme for begge token-typer)
    call.respond(service.getData())
}
```

**Token-krav:**
- ✅ OBO token med scope `leveattester.read` (har NAVident)
- ✅ M2M token med role `leveattester.read` (ingen NAVident)

**NAIS config:**
```yaml
# For OBO (saksbehandler)
- application: saksbehandling-app
  permissions:
    scopes:
      - "leveattester.read"

# For M2M (system)
- application: integration-app
  permissions:
    roles:
      - "leveattester.read"
```

---

## 📋 Pattern 3: M2M-only (Kun system-til-system)

**Bruk når:** Endepunktet er KUN for system-integrasjoner (ingen saksbehandler)

**Eksempel:**
```kotlin
get("healthcheck/detailed") {
    if (!call.requireRole("monitoring.read")) return@get
    
    // Ingen NAVident her - dette er system-til-system
    logger.info { "Health check called by monitoring system" }
    
    call.respond(healthService.getDetailedStatus())
}
```

**Token-krav:**
- ❌ OBO token blir avvist (selv med riktig scope)
- ✅ M2M token med role `monitoring.read`

**NAIS config:**
```yaml
- application: monitoring-system
  permissions:
    roles:
      - "monitoring.read"
```

---

## 🎯 Oppsummering

| Pattern | Funksjon | Aksepterer OBO? | Aksepterer M2M? | NAVident? |
|---------|----------|-----------------|-----------------|-----------|
| **1. OBO-only** | `requireScope("x")` | ✅ | ❌ | ✅ Påkrevd |
| **2. Fleksibel** | `requireScopeOrRole("x")` | ✅ | ✅ | ⚠️ Kun hvis OBO |
| **3. M2M-only** | `requireRole("x")` | ❌ | ✅ | ❌ Nei |

---

## 📝 Best Practices

### ✅ DO:
- Bruk `requireScope()` når du ALLTID trenger NAVident
- Bruk `requireScopeOrRole()` når begge token-typer er OK
- Bruk `getNavIdentOrNull()` for å sjekke om NAVident finnes
- Bruk samme permission-navn for scope og role hvis logikken er lik

### ❌ DON'T:
- Kall `getSaksbehandler()` etter `requireScopeOrRole()` (kan være M2M!)
- Kall `getSaksbehandler()` etter `requireRole()` (ALLTID M2M, mangler NAVident!)
- Bruk `requireScopeOrRole()` hvis du ALLTID trenger NAVident - bruk `requireScope()`

---

## 🔍 Debugging

### Sjekk token-type:
```kotlin
if (call.isOboToken()) {
    logger.info { "OBO token from user ${call.getSaksbehandler()}" }
} else if (call.isM2mToken()) {
    logger.info { "M2M token from system" }
}
```

### Hent NAVident sikkert:
```kotlin
val navIdent = call.getNavIdentOrNull()
if (navIdent != null) {
    // OBO token - har NAVident
    auditLog("User $navIdent accessed resource")
} else {
    // M2M token - ingen NAVident
    auditLog("System accessed resource")
}
```

