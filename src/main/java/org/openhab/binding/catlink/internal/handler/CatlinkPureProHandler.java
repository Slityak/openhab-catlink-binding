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

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.catlink.internal.api.CatlinkApiClient;
import org.openhab.binding.catlink.internal.api.CatlinkException;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.Command;

import com.google.gson.JsonObject;

/**
 * Handler for the {@code PUREPRO} water fountain. Reports water level, filter
 * life, temperature and the UV / heating / light / hair-cleaning states, and can
 * switch the run mode (flowing / eco / smart).
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public class CatlinkPureProHandler extends CatlinkBaseDeviceHandler {

    public CatlinkPureProHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected JsonObject fetchDetail(CatlinkApiClient api) throws CatlinkException {
        return api.getDeviceInfo(EP_PUREPRO_DETAIL, deviceId);
    }

    @Override
    protected void updateChannels(JsonObject d) {
        String runMode = str(d, "runMode");
        updateState(CH_MODE, new StringType(PUREPRO_MODES.getOrDefault(runMode, runMode)));
        updateState(CH_STATE, new StringType(runMode.isEmpty() ? str(d, "workStatus") : runMode));

        updateState(CH_WATER_LEVEL, new QuantityType<>(dbl(d, "waterLevelNum"), Units.PERCENT));
        updateState(CH_FILTER_LIFE, new QuantityType<>(dbl(d, "filterElementTimeCountdown"), Units.PERCENT));
        if (d.has("waterTemperature")) {
            updateState(CH_TEMPERATURE, new QuantityType<>(dbl(d, "waterTemperature"), SIUnits.CELSIUS));
        }

        updateState(CH_UV_ACTIVE, onOff(d, "ultravioletRaysSwitch"));
        updateState(CH_HEATING, onOff(d, "waterHeatSwitch"));
        updateState(CH_LIGHT_ACTIVE, onOff(d, "pureLightStatus"));

        String hair = str(d, "fluffyHairStatus").trim().toUpperCase(Locale.ROOT);
        updateState(CH_HAIR_CLEANING, OnOffType.from(!hair.isEmpty() && !"STOP".equals(hair)));

        if (d.has("onlineStatus")) {
            updateState(CH_ONLINE, onOff(d, "onlineStatus"));
        } else if (d.has("online")) {
            updateState(CH_ONLINE, onOff(d, "online"));
        }
    }

    @Override
    protected void pollExtra(CatlinkApiClient api) throws CatlinkException {
        updateLastLog(api, EP_PUREPRO_LOG, "pureLogTop5");
    }

    @Override
    protected void handleDeviceCommand(String ch, Command command, CatlinkApiClient api) throws CatlinkException {
        if (CH_MODE.equals(ch)) {
            String code = CatlinkScooperHandler.keyForValue(PUREPRO_MODES, command.toString());
            if (code != null) {
                java.util.Map<String, String> m = new java.util.TreeMap<>();
                m.put("runMode", code);
                m.put("deviceId", deviceId);
                if (api.post(EP_PUREPRO_RUNMODE, m)) {
                    refreshSoon();
                }
            }
        } else {
            logger.debug("Unhandled command {} on {}", command, ch);
        }
    }
}
