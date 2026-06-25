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

import static org.openhab.binding.catlink.internal.CatlinkBindingConstants.*;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.catlink.internal.api.CatlinkApiClient;
import org.openhab.binding.catlink.internal.api.CatlinkException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.Command;

import com.google.gson.JsonObject;

/**
 * Handler for a CatLink automatic {@code FEEDER}. Reports food-out / light /
 * power statuses and food weight, and can dispense a configurable number of
 * portions.
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public class CatlinkFeederHandler extends CatlinkBaseDeviceHandler {

    private volatile int portions = 1;

    public CatlinkFeederHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected JsonObject fetchDetail(CatlinkApiClient api) throws CatlinkException {
        return api.getDeviceInfo(EP_FEEDER_DETAIL, deviceId);
    }

    @Override
    protected void updateChannels(JsonObject d) {
        updateState(CH_STATE, new StringType(str(d, "foodOutStatus")));
        updateState(CH_AUTO_FILL, new StringType(str(d, "autoFillStatus")));
        updateState(CH_INDICATOR_LIGHT_STATUS, new StringType(str(d, "indicatorLightStatus")));
        updateState(CH_BREATH_LIGHT_STATUS, new StringType(str(d, "breathLightStatus")));
        updateState(CH_POWER_SUPPLY_STATUS, new StringType(str(d, "powerSupplyStatus")));
        if (d.has("keyLockStatus")) {
            updateState(CH_KEY_LOCK, onOff(d, "keyLockStatus"));
        }
        if (d.has("weight")) {
            updateState(CH_WEIGHT, new QuantityType<>(dbl(d, "weight"), SIUnits.GRAM));
        }
        if (d.has("online")) {
            updateState(CH_ONLINE, onOff(d, "online"));
        }
        updateState(CH_PORTIONS, new DecimalType(portions));

        String type = str(d, "currentErrorType");
        String msg = str(d, "currentErrorMessage");
        updateState(CH_ERROR, new StringType(msg.isEmpty() ? type : msg));
    }

    @Override
    protected void pollExtra(CatlinkApiClient api) throws CatlinkException {
        updateLastLog(api, EP_FEEDER_LOG, "feederLogTop5");
    }

    @Override
    protected void handleDeviceCommand(String ch, Command command, CatlinkApiClient api) throws CatlinkException {
        switch (ch) {
            case CH_PORTIONS -> {
                if (command instanceof DecimalType dt) {
                    portions = Math.max(1, Math.min(10, dt.intValue()));
                    updateState(CH_PORTIONS, new DecimalType(portions));
                }
            }
            case CH_FEED -> {
                if (command == OnOffType.ON) {
                    Map<String, String> m = new TreeMap<>();
                    m.put("footOutNum", String.valueOf(portions)); // API field is misspelled "footOutNum"
                    m.put("deviceId", deviceId);
                    if (api.post(EP_FEEDER_FOODOUT, m)) {
                        updateState(CH_FEED, OnOffType.OFF);
                        refreshSoon();
                    }
                }
            }
            default -> logger.debug("Unhandled command {} on {}", command, ch);
        }
    }
}
