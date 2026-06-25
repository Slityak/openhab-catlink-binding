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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.catlink.internal.api.CatlinkApiClient;
import org.openhab.binding.catlink.internal.api.CatlinkException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Thing;

import com.google.gson.JsonObject;

/**
 * Handler for the {@code VISUAL_PRO_ULTRA} (Scooper Pro Ultra). The cloud exposes
 * only a brief-info endpoint for this model, so support is limited to read-only
 * status channels (mirrors the Home Assistant integration).
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public class CatlinkProUltraHandler extends CatlinkBaseDeviceHandler {

    public CatlinkProUltraHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected JsonObject fetchDetail(CatlinkApiClient api) throws CatlinkException {
        return api.getDeviceInfo(EP_PROULTRA_INFO, deviceId);
    }

    @Override
    protected void updateChannels(JsonObject d) {
        String workStatus = str(d, "workStatus");
        if (!workStatus.isEmpty()) {
            updateState(CH_STATE, new StringType(WORK_STATUS.getOrDefault(workStatus.trim(), workStatus)));
        }
        updateState(CH_LITTER_REMAINING_DAYS, num(d, "litterCountdown"));
        updateState(CH_DEODORANT_COUNTDOWN, num(d, "deodorantCountdown"));

        int total = d.has("totalCleanTimes") ? intOf(d, "totalCleanTimes")
                : intOf(d, "inductionTimes") + intOf(d, "manualTimes");
        updateState(CH_TOTAL_CLEAN_TIME, new DecimalType(total));

        if (d.has("online")) {
            updateState(CH_ONLINE, onOff(d, "online"));
        }
    }
}
