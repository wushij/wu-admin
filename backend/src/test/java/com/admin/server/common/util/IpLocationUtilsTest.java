package com.admin.server.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IpLocationUtilsTest {

    @Test
    void testResolvePrivateIp() {
        assertEquals("内网IP", IpLocationUtils.resolve("127.0.0.1"));
        assertEquals("内网IP", IpLocationUtils.resolve("::1"));
        assertEquals("内网IP", IpLocationUtils.resolve("fe80::1"));
    }

    @Test
    void testResolveIpv4() {
        String loc = IpLocationUtils.resolve("114.114.114.114");
        assertNotNull(loc);
        assertNotEquals("未知", loc);
        System.out.println("114.114.114.114 location: " + loc);
    }

    @Test
    void testResolveIpv6UserIp() {
        String loc = IpLocationUtils.resolve("2602:feda:f392:fc99:d594:2631:8512:9ba5");
        assertNotNull(loc);
        System.out.println("2602:feda:... location: " + loc);
        assertNotEquals("未知", loc);
        assertTrue(loc.contains("美国") || loc.contains("纽约"));
    }
}
