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
package org.openhab.binding.catlink.internal;

import static org.openhab.binding.catlink.internal.CatlinkBindingConstants.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.catlink.internal.handler.CatlinkAccountHandler;
import org.openhab.binding.catlink.internal.handler.CatlinkC08Handler;
import org.openhab.binding.catlink.internal.handler.CatlinkCatHandler;
import org.openhab.binding.catlink.internal.handler.CatlinkFeederHandler;
import org.openhab.binding.catlink.internal.handler.CatlinkLitterBoxHandler;
import org.openhab.binding.catlink.internal.handler.CatlinkProUltraHandler;
import org.openhab.binding.catlink.internal.handler.CatlinkPureProHandler;
import org.openhab.binding.catlink.internal.handler.CatlinkScooperHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Creates handlers for CatLink things.
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.catlink", service = ThingHandlerFactory.class)
public class CatlinkHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED = Stream
            .concat(SUPPORTED_BRIDGE_THING_TYPES.stream(), SUPPORTED_DEVICE_THING_TYPES.stream())
            .collect(Collectors.toSet());

    private final HttpClient httpClient;

    @Activate
    public CatlinkHandlerFactory(@Reference HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID type = thing.getThingTypeUID();
        if (THING_TYPE_ACCOUNT.equals(type) && thing instanceof Bridge bridge) {
            return new CatlinkAccountHandler(bridge, httpClient);
        }
        if (THING_TYPE_SCOOPER.equals(type)) {
            return new CatlinkScooperHandler(thing);
        }
        if (THING_TYPE_LITTERBOX.equals(type)) {
            return new CatlinkLitterBoxHandler(thing);
        }
        if (THING_TYPE_C08.equals(type)) {
            return new CatlinkC08Handler(thing);
        }
        if (THING_TYPE_PROULTRA.equals(type)) {
            return new CatlinkProUltraHandler(thing);
        }
        if (THING_TYPE_FEEDER.equals(type)) {
            return new CatlinkFeederHandler(thing);
        }
        if (THING_TYPE_PUREPRO.equals(type)) {
            return new CatlinkPureProHandler(thing);
        }
        if (THING_TYPE_CAT.equals(type)) {
            return new CatlinkCatHandler(thing);
        }
        return null;
    }
}
