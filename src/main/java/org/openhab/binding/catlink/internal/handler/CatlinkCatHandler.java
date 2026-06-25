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

import java.time.LocalDate;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.catlink.internal.api.CatlinkApiClient;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Handler for a CatLink {@code cat} — the per-cat health profile and daily
 * statistics (toilet / drink / diet / sport). It is a child of the account
 * bridge and is keyed by the cloud {@code petId}.
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public class CatlinkCatHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(CatlinkCatHandler.class);

    private String petId = "";
    private @Nullable ScheduledFuture<?> pollJob;

    public CatlinkCatHandler(Thing thing) {
        super(thing);
    }

    private @Nullable CatlinkApiClient api() {
        Bridge bridge = getBridge();
        if (bridge != null && bridge.getHandler() instanceof CatlinkAccountHandler account) {
            return account.getApi();
        }
        return null;
    }

    @Override
    public void initialize() {
        Object p = getConfig().get(CFG_PET_ID);
        petId = p == null ? "" : p.toString();
        if (petId.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "petId is required");
            return;
        }
        updateStatus(ThingStatus.UNKNOWN);
        int refresh = 300;
        Bridge bridge = getBridge();
        if (bridge != null && bridge.getConfiguration().get(CFG_REFRESH) instanceof Number n) {
            refresh = Math.max(60, n.intValue() * 5); // cat stats change slowly; poll less often
        }
        pollJob = scheduler.scheduleWithFixedDelay(this::poll, 3, refresh, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> job = pollJob;
        if (job != null) {
            job.cancel(true);
            pollJob = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            scheduler.execute(this::poll);
        }
    }

    private void poll() {
        CatlinkApiClient client = api();
        if (client == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }
        try {
            JsonObject cat = findCat(client);
            if (cat == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cat " + petId + " not found");
                return;
            }
            updateStatus(ThingStatus.ONLINE);
            updateProfile(cat);
            updateSummary(client.getCatSummary(petId, LocalDate.now().toString()));
        } catch (Exception e) {
            logger.debug("Cat poll failed for {}", petId, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private @Nullable JsonObject findCat(CatlinkApiClient client) throws Exception {
        for (JsonElement el : client.getCats()) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            if (petId.equals(str(o, "id")) || petId.equals(str(o, "petId")) || petId.equals(str(o, "pet_id"))) {
                return o;
            }
        }
        return null;
    }

    private void updateProfile(JsonObject c) {
        if (c.has("weight")) {
            updateState(CH_CAT_WEIGHT, new QuantityType<>(dbl(c, "weight"), SIUnits.KILOGRAM));
        }
        updateState(CH_CAT_AGE_YEARS, new DecimalType(c.has("year") ? intOf(c, "year") : intOf(c, "age")));
        updateState(CH_CAT_AGE_MONTHS, new DecimalType(intOf(c, "month")));
        String gender = str(c, "gender");
        updateState(CH_CAT_GENDER, new StringType(CAT_GENDERS.getOrDefault(gender, gender)));
        updateState(CH_CAT_BREED, new StringType(str(c, "breedName")));
    }

    private void updateSummary(JsonObject data) {
        JsonObject summary = obj(data, "summary");
        String status = !str(summary, "statusDescription").isEmpty() ? str(summary, "statusDescription")
                : str(summary, "status");
        if (status.isEmpty()) {
            status = !str(data, "statusDescription").isEmpty() ? str(data, "statusDescription") : str(data, "status");
        }
        updateState(CH_CAT_STATUS, new StringType(status));

        JsonObject toilet = obj(data, "toilet");
        updateState(CH_CAT_TOILET_TIMES, new DecimalType(intOf(toilet, "times")));
        updateState(CH_CAT_TOILET_WEIGHT_AVG, num(toilet, "weightAvg"));
        updateState(CH_CAT_PEE_TIMES, new DecimalType(intOf(toilet, "peed")));
        updateState(CH_CAT_POO_TIMES, new DecimalType(intOf(toilet, "pood")));

        JsonObject drink = obj(data, "drink");
        updateState(CH_CAT_DRINK_TIMES, new DecimalType(intOf(drink, "times")));

        JsonObject diet = obj(data, "diet");
        updateState(CH_CAT_DIET_TIMES, new DecimalType(intOf(diet, "times")));
        updateState(CH_CAT_DIET_INTAKES, num(diet, "intakes"));

        JsonObject sport = obj(data, "sport");
        updateState(CH_CAT_SPORT_DURATION, num(sport, "activeDuration"));
    }

    // ---- json helpers ----

    private static JsonObject obj(JsonObject o, String key) {
        JsonElement el = o.get(key);
        return el != null && el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
    }

    private static String str(JsonObject o, String key) {
        JsonElement el = o.get(key);
        return el == null || el.isJsonNull() ? "" : el.getAsString();
    }

    private static int intOf(JsonObject o, String key) {
        try {
            String s = str(o, key);
            return s.isEmpty() ? 0 : (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double dbl(JsonObject o, String key) {
        try {
            String s = str(o, key);
            return s.isEmpty() ? 0d : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    private static DecimalType num(JsonObject o, String key) {
        return new DecimalType(java.math.BigDecimal.valueOf(dbl(o, key)));
    }
}
