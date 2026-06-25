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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Handler for the {@code C08} litter box (Open-X / Young Pro) — the richest
 * CatLink device. Adds the v3 action endpoint (clean/pave), litter type, safe
 * time, eight setting switches, nine notification toggles, Wi-Fi info and usage
 * statistics on top of the basic litter-box channels.
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public class CatlinkC08Handler extends CatlinkBaseDeviceHandler {

    public CatlinkC08Handler(Thing thing) {
        super(thing);
    }

    @Override
    protected JsonObject fetchDetail(CatlinkApiClient api) throws CatlinkException {
        return api.getDeviceInfo(EP_C08_INFO, deviceId);
    }

    @Override
    protected void updateChannels(JsonObject d) {
        String workStatus = str(d, "workStatus");
        updateState(CH_STATE, new StringType(WORK_STATUS.getOrDefault(workStatus.trim(), workStatus)));
        updateState(CH_MODE, new StringType(C08_MODES.getOrDefault(str(d, "workModel"), str(d, "workModel"))));

        updateState(CH_LITTER_WEIGHT, num(d, "catLitterWeight"));
        updateState(CH_LITTER_REMAINING_DAYS, num(d, "litterCountdown"));
        updateState(CH_DEODORANT_COUNTDOWN, num(d, "deodorantCountdown"));
        updateState(CH_TOTAL_CLEAN_TIME, new DecimalType(intOf(d, "inductionTimes") + intOf(d, "manualTimes")));
        updateState(CH_MANUAL_CLEAN_TIME, new DecimalType(intOf(d, "manualTimes")));

        if (d.has("online")) {
            updateState(CH_ONLINE, onOff(d, "online"));
        }

        // Settings (string-encoded booleans handled by the truthy() helper)
        updateState(CH_CHILD_LOCK, onOff(d, "keyLock"));
        updateState(CH_INDICATOR_LIGHT, onOff(d, "indicatorLight"));
        updateState(CH_KEYPAD_TONE, onOff(d, "paneltone"));
        updateState(CH_AUTO_BURIAL, onOff(d, "autoBurial"));
        updateState(CH_CONTINUOUS_CLEANING, onOff(d, "continuousCleaning"));
        updateState(CH_AUTO_PET_WEIGHT, onOff(d, "autoUpdatePetWeight"));
        updateState(CH_KITTEN_MODE, onOff(d, "kittenModel"));
        if (d.has("quietEnable")) {
            updateState(CH_QUIET_MODE, onOff(d, "quietEnable"));
        }

        // litterType comes back as an integer (0/2), not the "00"/"02" string used for writes.
        String litter = str(d, "litterType");
        String litterKey = litter.length() == 1 ? "0" + litter : litter;
        updateState(CH_LITTER_TYPE, new StringType(LITTER_TYPES.getOrDefault(litterKey, litter)));
        if (d.has("safeTime")) {
            updateState(CH_SAFE_TIME, new StringType(String.valueOf(intOf(d, "safeTime"))));
        }

        String err = !str(d, "currentMessage").isEmpty() ? str(d, "currentMessage") : str(d, "currentErrorMessage");
        updateState(CH_ERROR, new StringType(err));
    }

    @Override
    protected void pollExtra(CatlinkApiClient api) throws CatlinkException {
        updateLastLog(api, EP_LITTERBOX_LOG, "scooperLogTop5");

        JsonObject wifi = api.getData(EP_C08_WIFI, Map.of("deviceId", deviceId));
        JsonElement wi = wifi.get("wifiInfo");
        if (wi != null && wi.isJsonObject()) {
            JsonObject w = wi.getAsJsonObject();
            updateState(CH_WIFI_RSSI, num(w, "rssi"));
            String ssid = !str(w, "wifiName").isEmpty() ? str(w, "wifiName") : str(w, "wifi_name");
            updateState(CH_WIFI_SSID, new StringType(ssid));
        }

        JsonObject data = api.getData(EP_C08_COMPARE, Map.of("deviceId", deviceId));
        JsonElement cmp = data.get("compareData");
        if (cmp != null && cmp.isJsonObject()) {
            JsonObject c = cmp.getAsJsonObject();
            updateState(CH_STATS_TIMES, num(c, "times"));
            updateState(CH_STATS_WEIGHT_AVG, num(c, "weightAvg"));
            updateState(CH_STATS_DURATION_AVG, num(c, "durationAvg"));
        }

        for (JsonElement el : api.getDataArray(EP_C08_NOTICE_LIST, Map.of("deviceId", deviceId), "noticeConfigs")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject n = el.getAsJsonObject();
            String slug = slugForNoticeItem(str(n, "noticeItem"));
            if (slug != null) {
                updateState("notice-" + slug, onOff(n, "noticeSwitch"));
            }
        }
    }

    @Override
    protected void handleDeviceCommand(String ch, Command command, CatlinkApiClient api) throws CatlinkException {
        String value = command.toString();
        boolean done;
        switch (ch) {
            case CH_MODE -> {
                String code = CatlinkScooperHandler.keyForValue(C08_MODES, value);
                done = code != null && api.post(EP_LITTERBOX_CHANGE_MODE, with("workModel", code));
            }
            case CH_ACTION -> {
                String[] ab = C08_ACTIONS.get(value);
                if (ab == null) {
                    return;
                }
                Map<String, String> m = with("action", ab[0]);
                m.put("behavior", ab[1]);
                done = api.post(EP_C08_ACTION_V3, m);
            }
            case CH_LITTER_TYPE -> {
                String code = CatlinkScooperHandler.keyForValue(LITTER_TYPES, value);
                done = code != null && api.post(EP_C08_LITTER_SETTING, with("litterType", code));
            }
            case CH_SAFE_TIME -> done = api.post(EP_C08_SAFE_TIME, with("safeTime", value.trim()));
            case CH_CHILD_LOCK ->
                done = api.post(EP_C08_KEYLOCK, with("lockStatus", on(command) ? "LOCKED" : "UNLOCKED"));
            case CH_INDICATOR_LIGHT ->
                done = api.post(EP_C08_INDICATOR, with("status", on(command) ? "ALWAYS_OPEN" : "CLOSED"));
            case CH_KEYPAD_TONE -> {
                Map<String, String> m = with("panelTone", on(command) ? "ENABLED" : "DISABLED");
                m.put("kind", "00");
                done = api.post(EP_C08_KEYPAD_TONE, m);
            }
            case CH_AUTO_BURIAL -> done = api.post(EP_C08_AUTO_BURIAL, enable(command));
            case CH_CONTINUOUS_CLEANING -> done = api.post(EP_C08_CONTINUOUS, enable(command));
            case CH_KITTEN_MODE -> done = api.post(EP_C08_KITTY, enable(command));
            case CH_AUTO_PET_WEIGHT -> done = api.post(EP_C08_PET_WEIGHT, enable(command));
            case CH_QUIET_MODE -> {
                // Mirrors the HA integration, which (re)uses the autoBurial endpoint with a
                // time range for quiet mode. Verify against real hardware.
                Map<String, String> m = enable(command);
                m.put("times", "22:00-07:00");
                done = api.post(EP_C08_AUTO_BURIAL, m);
            }
            default -> {
                if (ch.startsWith("notice-")) {
                    String code = C08_NOTICE_ITEMS.get(ch.substring("notice-".length()));
                    if (code == null) {
                        return;
                    }
                    Map<String, String> m = new TreeMap<>();
                    m.put("noticeItem", code);
                    m.put("noticeSwitch", String.valueOf(on(command)));
                    m.put("deviceId", deviceId);
                    done = api.post(EP_C08_NOTICE_SET, m);
                } else {
                    logger.debug("Unhandled command {} on {}", command, ch);
                    return;
                }
            }
        }
        if (done) {
            refreshSoon();
        }
    }

    private static boolean on(Command command) {
        return command == OnOffType.ON;
    }

    private Map<String, String> with(String key, String value) {
        Map<String, String> m = new TreeMap<>();
        m.put(key, value);
        m.put("deviceId", deviceId);
        return m;
    }

    private Map<String, String> enable(Command command) {
        return with("enable", String.valueOf(on(command)));
    }

    private static @org.eclipse.jdt.annotation.Nullable String slugForNoticeItem(String code) {
        for (Map.Entry<String, String> e : C08_NOTICE_ITEMS.entrySet()) {
            if (e.getValue().equals(code)) {
                return e.getKey();
            }
        }
        return null;
    }
}
