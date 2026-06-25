/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.catlink.internal.handler;

import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.catlink.internal.api.CatlinkApiClient;
import org.openhab.binding.catlink.internal.config.CatlinkAccountConfiguration;
import org.openhab.binding.catlink.internal.discovery.CatlinkDiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge handler representing a CatLink account. Holds the shared
 * {@link CatlinkApiClient} that child device handlers and discovery use.
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public class CatlinkAccountHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(CatlinkAccountHandler.class);
    private final HttpClient httpClient;

    private @Nullable CatlinkApiClient api;

    public CatlinkAccountHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);
        this.httpClient = httpClient;
    }

    public @Nullable CatlinkApiClient getApi() {
        return api;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(CatlinkDiscoveryService.class);
    }

    @Override
    public void initialize() {
        CatlinkAccountConfiguration cfg = getConfigAs(CatlinkAccountConfiguration.class);
        if ((cfg.email.isBlank() && cfg.phone.isBlank()) || cfg.password.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "An email or phone number, and a password, are required");
            return;
        }
        api = new CatlinkApiClient(httpClient, cfg.region, cfg.email, cfg.phone, cfg.phoneIac, cfg.password,
                cfg.language);
        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(() -> {
            try {
                CatlinkApiClient client = api;
                if (client != null && client.login()) {
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Login failed");
                }
            } catch (Exception e) {
                logger.debug("CatLink login error", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        });
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Bridge has no channels of its own.
    }

    @Override
    public void dispose() {
        api = null;
    }
}
