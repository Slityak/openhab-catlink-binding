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

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.catlink.internal.api.CatlinkApiClient;
import org.openhab.binding.catlink.internal.api.CatlinkException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Common base for all CatLink device handlers. Owns the polling loop, bridge/API
 * plumbing and a set of JSON/state helpers; subclasses only describe how to fetch
 * the device detail, map it to channels and react to commands.
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public abstract class CatlinkBaseDeviceHandler extends BaseThingHandler {

    /** Field values that the cloud treats as "on"/true across the various boolean-ish settings. */
    private static final Set<String> TRUTHY = Set.of("1", "01", "true", "yes", "on", "open", "online", "locked",
            "always_open", "enabled", "enable");

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String deviceId = "";
    protected String deviceType = "";

    private @Nullable ScheduledFuture<?> pollJob;

    protected CatlinkBaseDeviceHandler(Thing thing) {
        super(thing);
    }

    protected @Nullable CatlinkApiClient api() {
        Bridge bridge = getBridge();
        if (bridge != null && bridge.getHandler() instanceof CatlinkAccountHandler account) {
            return account.getApi();
        }
        return null;
    }

    @Override
    public void initialize() {
        deviceId = stringConfig(CFG_DEVICE_ID);
        deviceType = stringConfig(CFG_DEVICE_TYPE);
        if (deviceType.isBlank()) {
            // discovery stores it as a property
            deviceType = thing.getProperties().getOrDefault(CFG_DEVICE_TYPE, "");
        }
        if (deviceId.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "deviceId is required");
            return;
        }
        updateStatus(ThingStatus.UNKNOWN);
        pollJob = scheduler.scheduleWithFixedDelay(this::poll, 2, refreshSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> job = pollJob;
        if (job != null) {
            job.cancel(true);
            pollJob = null;
        }
    }

    private int refreshSeconds() {
        int refresh = 60;
        Bridge bridge = getBridge();
        if (bridge != null && bridge.getConfiguration().get(CFG_REFRESH) instanceof Number n) {
            refresh = n.intValue();
        }
        return Math.max(15, refresh);
    }

    protected void poll() {
        CatlinkApiClient client = api();
        if (client == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }
        try {
            JsonObject d = fetchDetail(client);
            if (d.size() == 0) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "No device detail");
                return;
            }
            updateStatus(ThingStatus.ONLINE);
            updateChannels(d);
            pollExtra(client);
        } catch (Exception e) {
            logger.debug("Poll failed for {}", deviceId, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /** Fetch the device-specific detail object. */
    protected abstract JsonObject fetchDetail(CatlinkApiClient api) throws CatlinkException;

    /** Map the detail object to channels. */
    protected abstract void updateChannels(JsonObject detail);

    /** Optional extra polling (logs, stats); default no-op. */
    protected void pollExtra(CatlinkApiClient api) throws CatlinkException {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            scheduler.execute(this::poll);
            return;
        }
        CatlinkApiClient client = api();
        if (client == null) {
            return;
        }
        try {
            handleDeviceCommand(channelUID.getId(), command, client);
        } catch (Exception e) {
            logger.debug("Command {} on {} failed", command, channelUID, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /** Handle a non-refresh command. Default: nothing. */
    protected void handleDeviceCommand(String channelId, Command command, CatlinkApiClient api)
            throws CatlinkException {
    }

    /** Re-poll shortly after a command so the UI reflects the change. */
    protected void refreshSoon() {
        scheduler.schedule(this::poll, 4, TimeUnit.SECONDS);
    }

    /** Update the {@code last-log} channel from the first entry of a top-5 log endpoint. */
    protected void updateLastLog(CatlinkApiClient api, String endpoint, String key) throws CatlinkException {
        JsonArray logs = api.getDataArray(endpoint, java.util.Map.of("deviceId", deviceId), key);
        if (logs.size() > 0 && logs.get(0).isJsonObject()) {
            JsonObject e = logs.get(0).getAsJsonObject();
            String text = (str(e, "time") + " " + str(e, "event") + " " + str(e, "firstSection") + " "
                    + str(e, "secondSection")).trim().replaceAll("\\s+", " ");
            updateState(CH_LAST_LOG, new StringType(text));
        }
    }

    // ---- config / json helpers --------------------------------------------------------------

    protected String stringConfig(String key) {
        Object v = getConfig().get(key);
        return v == null ? "" : v.toString();
    }

    protected static String str(JsonObject o, String key) {
        JsonElement el = o.get(key);
        return el == null || el.isJsonNull() ? "" : el.getAsString();
    }

    protected static int intOf(JsonObject o, String key) {
        try {
            String s = str(o, key);
            return s.isEmpty() ? 0 : (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    protected static double dbl(JsonObject o, String key) {
        try {
            String s = str(o, key);
            return s.isEmpty() ? 0d : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    protected static DecimalType num(JsonObject o, String key) {
        return new DecimalType(BigDecimal.valueOf(dbl(o, key)));
    }

    /** True when the raw field value is one of the cloud's "on"/true tokens. */
    protected static boolean truthy(JsonObject o, String key) {
        return TRUTHY.contains(str(o, key).trim().toLowerCase(Locale.ROOT));
    }

    protected static OnOffType onOff(JsonObject o, String key) {
        return OnOffType.from(truthy(o, key));
    }
}
