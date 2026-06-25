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
package org.openhab.binding.catlink.internal.discovery;

import static org.openhab.binding.catlink.internal.CatlinkBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.catlink.internal.api.CatlinkApiClient;
import org.openhab.binding.catlink.internal.api.CatlinkDeviceSummary;
import org.openhab.binding.catlink.internal.handler.CatlinkAccountHandler;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Discovers all CatLink devices (litter boxes, feeders, fountains) and cats
 * belonging to the bridge account, dispatching each to the matching thing-type.
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
@Component(scope = ServiceScope.PROTOTYPE, service = CatlinkDiscoveryService.class)
public class CatlinkDiscoveryService extends AbstractThingHandlerDiscoveryService<CatlinkAccountHandler> {

    private static final int TIMEOUT_SECONDS = 30;
    private final Logger logger = LoggerFactory.getLogger(CatlinkDiscoveryService.class);

    public CatlinkDiscoveryService() {
        super(CatlinkAccountHandler.class, SUPPORTED_DEVICE_THING_TYPES, TIMEOUT_SECONDS, false);
    }

    @Override
    protected void startScan() {
        CatlinkApiClient api = thingHandler.getApi();
        if (api == null) {
            return;
        }
        ThingUID bridgeUID = thingHandler.getThing().getUID();
        try {
            for (CatlinkDeviceSummary dev : api.getDevices()) {
                ThingTypeUID type = thingTypeForDeviceType(dev.deviceType());
                ThingUID uid = new ThingUID(type, bridgeUID, sanitize(dev.id()));
                String label = dev.name().isEmpty() ? "CatLink " + dev.deviceType() + " " + dev.id() : dev.name();
                thingDiscovered(DiscoveryResultBuilder.create(uid).withBridge(bridgeUID).withLabel(label)
                        .withProperty(CFG_DEVICE_ID, dev.id()).withProperty(CFG_DEVICE_TYPE, dev.deviceType())
                        .withRepresentationProperty(CFG_DEVICE_ID).build());
            }
        } catch (Exception e) {
            logger.debug("CatLink device discovery failed", e);
        }
        try {
            discoverCats(api, bridgeUID);
        } catch (Exception e) {
            logger.debug("CatLink cat discovery failed", e);
        }
    }

    private void discoverCats(CatlinkApiClient api, ThingUID bridgeUID) throws Exception {
        for (JsonElement el : api.getCats()) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject c = el.getAsJsonObject();
            String id = first(c, "id", "petId", "pet_id");
            if (id.isEmpty()) {
                continue;
            }
            String name = first(c, "petName", "name", "nickname", "catName");
            ThingUID uid = new ThingUID(THING_TYPE_CAT, bridgeUID, "cat-" + sanitize(id));
            thingDiscovered(DiscoveryResultBuilder.create(uid).withBridge(bridgeUID)
                    .withLabel(name.isEmpty() ? "CatLink Cat " + id : name).withProperty(CFG_PET_ID, id)
                    .withRepresentationProperty(CFG_PET_ID).build());
        }
    }

    private static String first(JsonObject o, String... keys) {
        for (String k : keys) {
            JsonElement el = o.get(k);
            if (el != null && !el.isJsonNull()) {
                String s = el.getAsString();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        }
        return "";
    }

    private static String sanitize(String id) {
        return id.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
