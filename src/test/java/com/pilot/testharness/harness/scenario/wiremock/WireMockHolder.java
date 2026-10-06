package com.pilot.testharness.harness.scenario.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public final class WireMockHolder {

    private WireMockHolder() {
    }

    public static final WireMockServer SERVER =
            new WireMockServer(
                    wireMockConfig()
                            .dynamicPort()
            );

    static {
        SERVER.start();
    }
}