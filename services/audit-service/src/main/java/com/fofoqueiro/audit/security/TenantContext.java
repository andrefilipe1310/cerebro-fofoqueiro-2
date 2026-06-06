package com.fofoqueiro.audit.security;

import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();
    private TenantContext() {}
    public static void set(UUID id) { CURRENT.set(id); }
    public static UUID get() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }
}
