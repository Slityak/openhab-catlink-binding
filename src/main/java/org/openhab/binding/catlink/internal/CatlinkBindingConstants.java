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

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * Constants used across the CatLink binding.
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public class CatlinkBindingConstants {

    public static final String BINDING_ID = "catlink";

    // ---- Thing types ----
    public static final ThingTypeUID THING_TYPE_ACCOUNT = new ThingTypeUID(BINDING_ID, "account");
    public static final ThingTypeUID THING_TYPE_SCOOPER = new ThingTypeUID(BINDING_ID, "scooper");
    public static final ThingTypeUID THING_TYPE_LITTERBOX = new ThingTypeUID(BINDING_ID, "litterbox");
    public static final ThingTypeUID THING_TYPE_C08 = new ThingTypeUID(BINDING_ID, "c08");
    public static final ThingTypeUID THING_TYPE_PROULTRA = new ThingTypeUID(BINDING_ID, "proultra");
    public static final ThingTypeUID THING_TYPE_FEEDER = new ThingTypeUID(BINDING_ID, "feeder");
    public static final ThingTypeUID THING_TYPE_PUREPRO = new ThingTypeUID(BINDING_ID, "purepro");
    public static final ThingTypeUID THING_TYPE_CAT = new ThingTypeUID(BINDING_ID, "cat");

    public static final Set<ThingTypeUID> SUPPORTED_BRIDGE_THING_TYPES = Set.of(THING_TYPE_ACCOUNT);
    public static final Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES = Set.of(THING_TYPE_SCOOPER,
            THING_TYPE_LITTERBOX, THING_TYPE_C08, THING_TYPE_PROULTRA, THING_TYPE_FEEDER, THING_TYPE_PUREPRO,
            THING_TYPE_CAT);

    /** Maps the cloud {@code deviceType} string to the matching thing-type. */
    public static ThingTypeUID thingTypeForDeviceType(String deviceType) {
        switch (deviceType.toUpperCase()) {
            case "C08":
                return THING_TYPE_C08;
            case "LITTER_BOX_599":
                return THING_TYPE_LITTERBOX;
            case "VISUAL_PRO_ULTRA":
                return THING_TYPE_PROULTRA;
            case "FEEDER":
                return THING_TYPE_FEEDER;
            case "PUREPRO":
                return THING_TYPE_PUREPRO;
            case "SCOOPER":
            default:
                // Unknown litter-box-like devices fall back to the generic scooper handler.
                return THING_TYPE_SCOOPER;
        }
    }

    // ---- Channel IDs (shared where the meaning matches) ----
    public static final String CH_STATE = "state";
    public static final String CH_MODE = "mode";
    public static final String CH_ACTION = "action";
    public static final String CH_LITTER_WEIGHT = "litter-weight";
    public static final String CH_LITTER_REMAINING_DAYS = "litter-remaining-days";
    public static final String CH_TEMPERATURE = "temperature";
    public static final String CH_HUMIDITY = "humidity";
    public static final String CH_TOTAL_CLEAN_TIME = "total-clean-time";
    public static final String CH_MANUAL_CLEAN_TIME = "manual-clean-time";
    public static final String CH_DEODORANT_COUNTDOWN = "deodorant-countdown";
    public static final String CH_KEY_LOCK = "key-lock";
    public static final String CH_ERROR = "error";
    public static final String CH_ALARM = "alarm";
    public static final String CH_ONLINE = "online";
    public static final String CH_LAST_LOG = "last-log";
    public static final String CH_OCCUPIED = "occupied";

    // LITTER_BOX_599 / garbage
    public static final String CH_GARBAGE_STATUS = "garbage-status";
    public static final String CH_BOX_FULL_SENSITIVITY = "box-full-sensitivity";
    public static final String CH_REPLACE_GARBAGE_BAG = "replace-garbage-bag";
    public static final String CH_RESET_LITTER = "reset-litter";
    public static final String CH_RESET_DEODORANT = "reset-deodorant";

    // C08
    public static final String CH_LITTER_TYPE = "litter-type";
    public static final String CH_SAFE_TIME = "safe-time";
    public static final String CH_QUIET_MODE = "quiet-mode";
    public static final String CH_AUTO_BURIAL = "auto-burial";
    public static final String CH_CONTINUOUS_CLEANING = "continuous-cleaning";
    public static final String CH_CHILD_LOCK = "child-lock";
    public static final String CH_INDICATOR_LIGHT = "indicator-light";
    public static final String CH_KEYPAD_TONE = "keypad-tone";
    public static final String CH_AUTO_PET_WEIGHT = "auto-pet-weight";
    public static final String CH_KITTEN_MODE = "kitten-mode";
    public static final String CH_WIFI_RSSI = "wifi-rssi";
    public static final String CH_WIFI_SSID = "wifi-ssid";
    public static final String CH_STATS_TIMES = "stats-times";
    public static final String CH_STATS_WEIGHT_AVG = "stats-weight-avg";
    public static final String CH_STATS_DURATION_AVG = "stats-duration-avg";

    // Feeder
    public static final String CH_FEED = "feed";
    public static final String CH_PORTIONS = "portions";
    public static final String CH_WEIGHT = "weight";
    public static final String CH_AUTO_FILL = "auto-fill";
    public static final String CH_INDICATOR_LIGHT_STATUS = "indicator-light-status";
    public static final String CH_BREATH_LIGHT_STATUS = "breath-light-status";
    public static final String CH_POWER_SUPPLY_STATUS = "power-supply-status";

    // PurePro fountain
    public static final String CH_WATER_LEVEL = "water-level";
    public static final String CH_FILTER_LIFE = "filter-life";
    public static final String CH_UV_ACTIVE = "uv-active";
    public static final String CH_HEATING = "heating";
    public static final String CH_LIGHT_ACTIVE = "light-active";
    public static final String CH_HAIR_CLEANING = "hair-cleaning";

    // Cat (per-cat health)
    public static final String CH_CAT_STATUS = "status";
    public static final String CH_CAT_WEIGHT = "weight";
    public static final String CH_CAT_AGE_YEARS = "age-years";
    public static final String CH_CAT_AGE_MONTHS = "age-months";
    public static final String CH_CAT_GENDER = "gender";
    public static final String CH_CAT_BREED = "breed";
    public static final String CH_CAT_TOILET_TIMES = "toilet-times";
    public static final String CH_CAT_TOILET_WEIGHT_AVG = "toilet-weight-avg";
    public static final String CH_CAT_PEE_TIMES = "pee-times";
    public static final String CH_CAT_POO_TIMES = "poo-times";
    public static final String CH_CAT_DRINK_TIMES = "drink-times";
    public static final String CH_CAT_DIET_TIMES = "diet-times";
    public static final String CH_CAT_DIET_INTAKES = "diet-intakes";
    public static final String CH_CAT_SPORT_DURATION = "sport-duration";

    // ---- Account (bridge) config keys ----
    public static final String CFG_EMAIL = "email";
    public static final String CFG_PHONE = "phone";
    public static final String CFG_PHONE_IAC = "phoneIac";
    public static final String CFG_PASSWORD = "password";
    public static final String CFG_REGION = "region";
    public static final String CFG_LANGUAGE = "language";
    public static final String CFG_REFRESH = "refreshInterval";

    // ---- Device (thing) config keys / properties ----
    public static final String CFG_DEVICE_ID = "deviceId";
    public static final String CFG_DEVICE_TYPE = "deviceType";
    public static final String CFG_EMPTY_WEIGHT = "emptyWeight";
    public static final String CFG_PET_ID = "petId";

    // ---- CatLink cloud API ----

    /** Regional API base URLs. */
    public static final Map<String, String> API_SERVERS = Map.of("global", "https://app.catlinks.cn/api/", "china",
            "https://app-sh.catlinks.cn/api/", "usa", "https://app-usa.catlinks.cn/api/", "singapore",
            "https://app-sgp.catlinks.cn/api/");

    /** App-embedded signing key used for the MD5 request signature. */
    public static final String SIGN_KEY = "00109190907746a7ad0e2139b6d09ce47551770157fe4ac5922f3a5454c82712";

    /** App-embedded RSA public key (DER / X.509, base64) used to encrypt the password. */
    public static final String RSA_PUBLIC_KEY = ""
            + "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCCA9I+iEl2AI8dnhdwwxPxHVK8iNAt6aTq6UhNsLsguWS5qtbLnuGz2RQdfNS"
            + "aKSU2B6D/vE2gb1fM6f1A5cKndqF/riWGWn1EfL3FFQZduOTxoA0RTQzhrTa5LHcJ/an/NuHUwShwIOij0Mf4g8faTe4FT7/HdA"
            + "oK7uW0cG9mZwIDAQAB";

    // HTTP headers mimicking the official app
    public static final String UA = "CATLINK/4.1.5 (iPhone; iOS 26.2.1; Scale/3.00)";
    public static final String APP_VERSION = "4.1.5";
    public static final String SYSTEM_VERSION = "26.2.1";

    // ---- Endpoints (relative to the regional base URL) ----
    public static final String EP_LOGIN = "login/password";
    public static final String EP_LOGIN_EMAIL = "login/email";
    public static final String EP_DEVICE_LIST = "token/device/union/list/sorted";
    public static final String EP_CONSUMABLE_RESET = "token/device/union/consumableReset";

    // Generic scooper
    public static final String EP_DEVICE_INFO = "token/device/info";
    public static final String EP_CHANGE_MODE = "token/device/changeMode";
    public static final String EP_ACTION_CMD = "token/device/actionCmd";
    public static final String EP_SCOOPER_LOG = "token/device/scooper/stats/log/top5";

    // LITTER_BOX_599
    public static final String EP_LITTERBOX_INFO = "token/litterbox/info";
    public static final String EP_LITTERBOX_CHANGE_MODE = "token/litterbox/changeMode";
    public static final String EP_LITTERBOX_ACTION = "token/litterbox/actionCmd";
    public static final String EP_LITTERBOX_GARBAGE = "token/litterbox/replaceGarbageBagCmd";
    public static final String EP_LITTERBOX_BOXFULL = "token/litterbox/boxFullSetting";
    public static final String EP_LITTERBOX_LOG = "token/litterbox/stats/log/top5";

    // C08
    public static final String EP_C08_INFO = "token/litterbox/info/c08";
    public static final String EP_C08_ACTION_V3 = "token/litterbox/actionCmd/v3";
    public static final String EP_C08_PET_WEIGHT = "token/litterbox/pet/weight/autoUpdate";
    public static final String EP_C08_LITTER_SETTING = "token/litterbox/catLitterSetting";
    public static final String EP_C08_AUTO_BURIAL = "token/litterbox/deepClean/autoBurial";
    public static final String EP_C08_CONTINUOUS = "token/litterbox/deepClean/continuousCleaning";
    public static final String EP_C08_KITTY = "token/litterbox/kittyModelSwitch";
    public static final String EP_C08_KEYLOCK = "token/litterbox/keyLock";
    public static final String EP_C08_INDICATOR = "token/litterbox/indicatorLightSetting";
    public static final String EP_C08_KEYPAD_TONE = "token/litterbox/keypadTone";
    public static final String EP_C08_SAFE_TIME = "token/litterbox/safeTimeSetting";
    public static final String EP_C08_NOTICE_SET = "token/litterbox/noticeConfig/set";
    public static final String EP_C08_NOTICE_LIST = "token/litterbox/noticeConfig/list/c08";
    public static final String EP_C08_COMPARE = "token/litterbox/stats/data/compare/v2";
    public static final String EP_C08_WIFI = "token/litterbox/wifi/info";

    // Visual Pro Ultra
    public static final String EP_PROULTRA_INFO = "token/visualScooper/briefInfo";

    // Feeder
    public static final String EP_FEEDER_DETAIL = "token/device/feeder/detail";
    public static final String EP_FEEDER_FOODOUT = "token/device/feeder/foodOut";
    public static final String EP_FEEDER_LOG = "token/device/feeder/stats/log/top5";

    // PurePro
    public static final String EP_PUREPRO_DETAIL = "token/device/purepro/detail";
    public static final String EP_PUREPRO_RUNMODE = "token/device/purepro/runMode";
    public static final String EP_PUREPRO_LOG = "token/device/purepro/stats/log/top5";

    // Per-cat health (account-level)
    public static final String EP_PET_CATS = "token/pet/health/v3/cats";
    public static final String EP_PET_SUMMARY = "token/pet/health/v3/summarySimple";

    /** returnCode for an expired / illegal token. */
    public static final int RC_ILLEGAL_TOKEN = 1002;

    // ---- Enums (code -> label) ----
    public static final Map<String, String> SCOOPER_MODES = Map.of("00", "auto", "01", "manual", "02", "time", "03",
            "empty");
    public static final Map<String, String> SCOOPER_ACTIONS = Map.of("00", "pause", "01", "start");
    public static final Map<String, String> LITTERBOX_MODES = Map.of("00", "auto", "01", "manual", "02", "time");
    public static final Map<String, String> LITTERBOX_ACTIONS = Map.of("00", "pause", "01", "start");
    public static final Map<String, String> C08_MODES = Map.of("00", "auto", "01", "manual", "02", "scheduled");
    public static final Map<String, String> WORK_STATUS = Map.of("00", "idle", "01", "running", "02", "need_reset");
    public static final Map<String, String> GARBAGE_STATUS = Map.of("00", "normal", "02", "movement_started", "03",
            "moving");
    public static final Map<String, String> LITTER_TYPES = Map.of("00", "bentonite", "02", "mixed");
    public static final Map<String, String> PUREPRO_MODES = Map.of("CONTINUOUS_SPRING", "flowing",
            "INTERMITTENT_SPRING", "eco", "INDUCTION_SPRING", "smart");
    public static final Map<String, String> CAT_GENDERS = Map.of("1", "male", "2", "female", "3", "neutered_male", "4",
            "neutered_female");

    /**
     * C08 action channel labels -&gt; ({@code action}, {@code behavior}) param pair for the v3 endpoint.
     */
    public static final Map<String, String[]> C08_ACTIONS = Map.of("clean-start", new String[] { "RUN", "CLEAN" },
            "clean-pause", new String[] { "PAUSE", "CLEAN" }, "clean-cancel", new String[] { "CANCEL", "CLEAN" },
            "pave-start", new String[] { "RUN", "PAVE" }, "pave-pause", new String[] { "PAUSE", "PAVE" });

    /** C08 notice slug -&gt; API noticeItem code. */
    public static final Map<String, String> C08_NOTICE_ITEMS = Map.ofEntries(
            Map.entry("cat-came", "LITTERBOX_599_CAT_CAME"), Map.entry("box-full", "LITTERBOX_599_BOX_FULL"),
            Map.entry("replace-garbage-bag", "REPLACE_GARBAGE_BAG"), Map.entry("wash-scooper", "WASH_SCOOPER"),
            Map.entry("replace-deodorant", "REPLACE_DEODORANT"),
            Map.entry("litter-not-enough", "LITTERBOX_599_CAT_LITTER_NOT_ENOUGH"),
            Map.entry("sandbox-not-enough", "LITTERBOX_599_SANDBOX_NOT_ENOUGHT"),
            Map.entry("anti-pinch", "LITTERBOX_599_ANTI_PINCH"),
            Map.entry("firmware-updated", "LITTERBOX_599_FIRMWARE_UPDATED"));

    private CatlinkBindingConstants() {
    }
}
