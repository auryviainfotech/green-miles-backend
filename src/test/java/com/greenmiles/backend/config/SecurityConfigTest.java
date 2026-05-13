package com.greenmiles.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SecurityConfigTest {

    @Test
    void expandOriginsAddsHttpWhenOnlyHttpsConfigured() {
        List<String> expanded = SecurityConfig.expandOriginsWithSchemeMirror(List.of("https://a41.03e.mytemp.website"));
        assertTrue(expanded.contains("https://a41.03e.mytemp.website"));
        assertTrue(expanded.contains("http://a41.03e.mytemp.website"));
        assertEquals(2, expanded.size());
    }

    @Test
    void expandOriginsAddsHttpsWhenOnlyHttpRemoteConfigured() {
        List<String> expanded = SecurityConfig.expandOriginsWithSchemeMirror(List.of("http://a41.03e.mytemp.website"));
        assertTrue(expanded.contains("http://a41.03e.mytemp.website"));
        assertTrue(expanded.contains("https://a41.03e.mytemp.website"));
        assertEquals(2, expanded.size());
    }

    @Test
    void expandOriginsDoesNotMirrorLocalhostHttp() {
        List<String> expanded = SecurityConfig.expandOriginsWithSchemeMirror(List.of("http://localhost:5173"));
        assertEquals(List.of("http://localhost:5173"), expanded);
    }
}
