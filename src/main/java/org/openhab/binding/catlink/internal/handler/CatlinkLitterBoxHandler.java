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
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.Command;

import com.google.gson.JsonObject;

/**
 * Handler for the {@code LITTER_BOX_599} (Scooper C1) litter box, using the
 * {@code token/litterbox/*} endpoints plus garbage-bag, box-full-sensitivity and
 * consumable-reset commands.
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public class CatlinkLitterBoxHandler extends CatlinkBaseDeviceHandler {

    public CatlinkLitterBoxHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected JsonObject fetchDetail(CatlinkApiClient api) throws CatlinkException {
        return api.getDeviceInfo(EP_LITTERBOX_INFO, deviceId);
    }

    @Override
    protected void updateChannels(JsonObject d) {
        String workStatus = str(d, "workStatus");
        updateState(CH_STATE, new StringType(WORK_STATUS.getOrDefault(workStatus.trim(), workStatus)));
        updateState(CH_MODE, new StringType(LITTERBOX_MODES.getOrDefault(str(d, "workModel"), str(d, "workModel"))));

        updateState(CH_LITTER_WEIGHT, num(d, "catLitterWeight"));
        updateState(CH_LITTER_REMAINING_DAYS, num(d, "litterCountdown"));
        updateState(CH_DEODORANT_COUNTDOWN, num(d, "deodorantCountdown"));

        updateState(CH_TOTAL_CLEAN_TIME, new DecimalType(intOf(d, "inductionTimes") + intOf(d, "manualTimes")));
        updateState(CH_MANUAL_CLEAN_TIME, new DecimalType(intOf(d, "manualTimes")));

        if (d.has("keyLock")) {
            updateState(CH_KEY_LOCK, onOff(d, "keyLock"));
        }
        if (d.has("online")) {
            updateState(CH_ONLINE, onOff(d, "online"));
        }
        String garbage = str(d, "garbageStatus");
        updateState(CH_GARBAGE_STATUS, new StringType(GARBAGE_STATUS.getOrDefault(garbage.trim(), garbage)));

        String sens = str(d, "boxFullSensitivity"); // "LEVEL_02" or a bare integer
        String level = sens.startsWith("LEVEL_") ? sens.substring(6) : sens;
        if (!level.isEmpty()) {
            try {
                updateState(CH_BOX_FULL_SENSITIVITY, new StringType(String.valueOf(Integer.parseInt(level))));
            } catch (NumberFormatException ignored) {
            }
        }
        updateState(CH_ALARM, new StringType(str(d, "alarmStatus")));

        String err = !str(d, "currentMessage").isEmpty() ? str(d, "currentMessage") : str(d, "currentErrorMessage");
        if (err.isEmpty()) {
            err = str(d, "currentError");
        }
        updateState(CH_ERROR, new StringType(err));
    }

    @Override
    protected void pollExtra(CatlinkApiClient api) throws CatlinkException {
        updateLastLog(api, EP_LITTERBOX_LOG, "scooperLogTop5");
    }

    @Override
    protected void handleDeviceCommand(String ch, Command command, CatlinkApiClient api) throws CatlinkException {
        switch (ch) {
            case CH_MODE -> {
                String code = CatlinkScooperHandler.keyForValue(LITTERBOX_MODES, command.toString());
                if (code != null && api.post(EP_LITTERBOX_CHANGE_MODE, with("workModel", code))) {
                    refreshSoon();
                }
            }
            case CH_ACTION -> {
                String code = CatlinkScooperHandler.keyForValue(LITTERBOX_ACTIONS, command.toString());
                if (code != null && api.post(EP_LITTERBOX_ACTION, with("cmd", code))) {
                    refreshSoon();
                }
            }
            case CH_BOX_FULL_SENSITIVITY -> {
                String level = "LEVEL_0" + command.toString().trim();
                if (api.post(EP_LITTERBOX_BOXFULL, with("level", level))) {
                    refreshSoon();
                }
            }
            case CH_REPLACE_GARBAGE_BAG -> {
                String enable = command == OnOffType.ON ? "1" : "0";
                if (api.post(EP_LITTERBOX_GARBAGE, with("enable", enable))) {
                    refreshSoon();
                }
            }
            case CH_RESET_LITTER -> {
                if (command == OnOffType.ON && api.post(EP_CONSUMABLE_RESET, consumable("CAT_LITTER"))) {
                    updateState(CH_RESET_LITTER, OnOffType.OFF);
                    refreshSoon();
                }
            }
            case CH_RESET_DEODORANT -> {
                if (command == OnOffType.ON && api.post(EP_CONSUMABLE_RESET, consumable("DEODORIZER_02"))) {
                    updateState(CH_RESET_DEODORANT, OnOffType.OFF);
                    refreshSoon();
                }
            }
            default -> logger.debug("Unhandled command {} on {}", command, ch);
        }
    }

    private Map<String, String> with(String key, String value) {
        Map<String, String> m = new TreeMap<>();
        m.put(key, value);
        m.put("deviceId", deviceId);
        return m;
    }

    private Map<String, String> consumable(String type) {
        Map<String, String> m = new TreeMap<>();
        m.put("consumablesType", type);
        m.put("deviceId", deviceId);
        m.put("deviceType", deviceType.isBlank() ? "LITTER_BOX_599" : deviceType);
        return m;
    }
}
