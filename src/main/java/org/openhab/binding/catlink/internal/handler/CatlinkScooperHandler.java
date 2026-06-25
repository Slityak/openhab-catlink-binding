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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.catlink.internal.api.CatlinkApiClient;
import org.openhab.binding.catlink.internal.api.CatlinkException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.Command;

import com.google.gson.JsonObject;

/**
 * Handler for a generic CatLink scooper litter box ({@code deviceType SCOOPER}),
 * using the {@code token/device/*} endpoints. Also serves as the fallback for
 * unrecognised litter-box-like devices.
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public class CatlinkScooperHandler extends CatlinkBaseDeviceHandler {

    public CatlinkScooperHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected JsonObject fetchDetail(CatlinkApiClient api) throws CatlinkException {
        return api.getDeviceDetail(deviceId);
    }

    @Override
    protected void updateChannels(JsonObject d) {
        String workStatus = str(d, "workStatus");
        updateState(CH_STATE, new StringType(WORK_STATUS.getOrDefault(workStatus.trim(), workStatus)));
        updateState(CH_MODE, new StringType(SCOOPER_MODES.getOrDefault(str(d, "workModel"), str(d, "workModel"))));

        updateState(CH_LITTER_WEIGHT, num(d, "catLitterWeight"));
        updateState(CH_LITTER_REMAINING_DAYS, num(d, "litterCountdown"));
        updateState(CH_DEODORANT_COUNTDOWN, num(d, "deodorantCountdown"));

        int total = intOf(d, "inductionTimes") + intOf(d, "manualTimes");
        updateState(CH_TOTAL_CLEAN_TIME, new DecimalType(total));
        updateState(CH_MANUAL_CLEAN_TIME, new DecimalType(intOf(d, "manualTimes")));

        if (d.has("temperature")) {
            updateState(CH_TEMPERATURE, new QuantityType<>(dbl(d, "temperature"), SIUnits.CELSIUS));
        }
        if (d.has("humidity")) {
            updateState(CH_HUMIDITY, new QuantityType<>(dbl(d, "humidity"), Units.PERCENT));
        }
        if (d.has("keyLock")) {
            updateState(CH_KEY_LOCK, onOff(d, "keyLock"));
        }
        if (d.has("online")) {
            updateState(CH_ONLINE, onOff(d, "online"));
        }
        updateState(CH_ALARM, new StringType(str(d, "alarmStatus")));

        String err = !str(d, "currentMessage").isEmpty() ? str(d, "currentMessage") : str(d, "currentErrorMessage");
        updateState(CH_ERROR, new StringType(err));
    }

    @Override
    protected void pollExtra(CatlinkApiClient api) throws CatlinkException {
        updateLastLog(api, EP_SCOOPER_LOG, "scooperLogTop5");
    }

    @Override
    protected void handleDeviceCommand(String ch, Command command, CatlinkApiClient api) throws CatlinkException {
        switch (ch) {
            case CH_MODE -> {
                String code = keyForValue(SCOOPER_MODES, command.toString());
                if (code != null && api.post(EP_CHANGE_MODE, mode(code))) {
                    refreshSoon();
                }
            }
            case CH_ACTION -> {
                String code = keyForValue(SCOOPER_ACTIONS, command.toString());
                if (code != null && api.post(EP_ACTION_CMD, cmd(code))) {
                    refreshSoon();
                }
            }
            default -> logger.debug("Unhandled command {} on {}", command, ch);
        }
    }

    private Map<String, String> mode(String workModel) {
        Map<String, String> m = new TreeMap<>();
        m.put("workModel", workModel);
        m.put("deviceId", deviceId);
        return m;
    }

    private Map<String, String> cmd(String code) {
        Map<String, String> m = new TreeMap<>();
        m.put("cmd", code);
        m.put("deviceId", deviceId);
        return m;
    }

    static @Nullable String keyForValue(Map<String, String> map, String value) {
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getValue().equals(value)) {
                return e.getKey();
            }
        }
        return null;
    }
}
