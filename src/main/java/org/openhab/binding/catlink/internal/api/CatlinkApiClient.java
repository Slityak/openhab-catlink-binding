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
package org.openhab.binding.catlink.internal.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.catlink.internal.CatlinkBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Talks to the CatLink cloud. Stateless except for the auth token, which it
 * manages itself (logging in lazily and re-logging in on a {@code 1002} reply).
 *
 * <p>
 * The low-level transport reproduces the official app: every request is signed
 * (MD5 over the sorted params plus the embedded key) and carries the iOS app
 * headers. Higher-level helpers ({@link #getData}, {@link #getDeviceInfo},
 * {@link #post}) let the device handlers stay endpoint-agnostic.
 *
 * Note: the cloud allows only a single active session per account, so use a
 * dedicated (shared) sub-account for the binding.
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public class CatlinkApiClient {

    private final Logger logger = LoggerFactory.getLogger(CatlinkApiClient.class);

    private final HttpClient http;
    private final String baseUrl;
    private final String email;
    private final String phone;
    private final String phoneIac;
    private final String encryptedPassword;
    private final String language;

    private volatile String token = "";
    /** Cached index of the email-login strategy that worked, or -1 if unknown. */
    private volatile int emailStrategy = -1;

    public CatlinkApiClient(HttpClient http, String region, String email, String phone, String phoneIac,
            String plainPassword, String language) {
        this.http = http;
        this.baseUrl = CatlinkBindingConstants.API_SERVERS.getOrDefault(region,
                CatlinkBindingConstants.API_SERVERS.getOrDefault("global", ""));
        this.email = email;
        this.phone = phone;
        this.phoneIac = phoneIac;
        this.encryptedPassword = CatlinkCrypto.encryptPassword(plainPassword, CatlinkBindingConstants.RSA_PUBLIC_KEY);
        this.language = language;
    }

    private String url(String api) {
        return baseUrl.endsWith("/") ? baseUrl + api : baseUrl + "/" + api;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Build the encoded query/body string, computing the sign over raw values. */
    private String buildBody(Map<String, String> params, boolean withToken) {
        TreeMap<String, String> all = new TreeMap<>(params);
        all.put("noncestr", Long.toString(System.currentTimeMillis()));
        if (withToken && !token.isEmpty()) {
            all.put("token", token);
        }
        String sign = CatlinkCrypto.sign(all);
        all.put("sign", sign);

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> e : all.entrySet()) {
            parts.add(enc(e.getKey()) + "=" + enc(e.getValue()));
        }
        return String.join("&", parts);
    }

    private JsonObject request(String api, Map<String, String> params, HttpMethod method, boolean withToken)
            throws CatlinkException {
        return request(api, params, method, withToken, false);
    }

    private JsonObject request(String api, Map<String, String> params, HttpMethod method, boolean withToken,
            boolean retried) throws CatlinkException {
        String body = buildBody(params, withToken);
        try {
            Request req;
            if (method == HttpMethod.GET) {
                req = http.newRequest(url(api) + "?" + body).method(HttpMethod.GET);
            } else {
                req = http.newRequest(url(api)).method(HttpMethod.POST).content(new StringContentProvider(body),
                        "application/x-www-form-urlencoded");
            }
            req.header("language", language);
            req.header("User-Agent", CatlinkBindingConstants.UA);
            req.header("app_version", CatlinkBindingConstants.APP_VERSION);
            req.header("system_version", CatlinkBindingConstants.SYSTEM_VERSION);
            req.header("token", token);
            req.timeout(60, TimeUnit.SECONDS);

            ContentResponse resp = req.send();
            String content = resp.getContentAsString();
            JsonObject json = JsonParser.parseString(content == null || content.isEmpty() ? "{}" : content)
                    .getAsJsonObject();

            if (logger.isDebugEnabled()) {
                logger.debug("API {} {} -> {}", method, api, json);
            }

            int rc = json.has("returnCode") ? json.get("returnCode").getAsInt() : 0;
            if (rc == CatlinkBindingConstants.RC_ILLEGAL_TOKEN && !retried) {
                logger.debug("Token expired, re-login and retry {}", api);
                if (login()) {
                    return request(api, params, method, withToken, true);
                }
            }
            return json;
        } catch (Exception e) {
            throw new CatlinkException("Request to " + api + " failed: " + e.getMessage(), e);
        }
    }

    /** Logs in and stores the token. Returns true on success. */
    public boolean login() throws CatlinkException {
        token = "";
        return email.isBlank() ? loginPhone() : loginEmail();
    }

    private boolean loginPhone() throws CatlinkException {
        Map<String, String> pms = new TreeMap<>();
        pms.put("platform", "ANDROID");
        pms.put("internationalCode", phoneIac);
        pms.put("mobile", phone);
        pms.put("password", encryptedPassword);

        JsonObject rsp = request(CatlinkBindingConstants.EP_LOGIN, pms, HttpMethod.POST, false);
        if (extractToken(rsp)) {
            return true;
        }
        logger.warn("CatLink phone login failed for {}: returnCode={}, msg={}", phone,
                rsp.has("returnCode") ? rsp.get("returnCode") : "?", msgOf(rsp));
        return false;
    }

    /**
     * Email login is not documented by any public client, so we probe a handful of
     * plausible payloads (different identifier field / endpoint) and cache the one
     * that returns a token. Each attempt is logged so the working variant can be
     * pinned down from the debug log.
     */
    private boolean loginEmail() throws CatlinkException {
        LoginAttempt[] attempts = emailAttempts();
        // Try the previously successful strategy first, if any.
        if (emailStrategy >= 0 && emailStrategy < attempts.length && tryEmail(attempts[emailStrategy])) {
            return true;
        }
        for (int i = 0; i < attempts.length; i++) {
            if (i == emailStrategy) {
                continue;
            }
            if (tryEmail(attempts[i])) {
                emailStrategy = i;
                return true;
            }
        }
        logger.warn("CatLink email login failed for {} - none of the {} strategies returned a token", email,
                attempts.length);
        return false;
    }

    private boolean tryEmail(LoginAttempt a) throws CatlinkException {
        JsonObject rsp = request(a.endpoint, a.params, HttpMethod.POST, false);
        if (extractToken(rsp)) {
            logger.info("CatLink email login succeeded with strategy '{}' ({} {})", a.label, a.endpoint,
                    a.params.keySet());
            return true;
        }
        logger.info("CatLink email login strategy '{}' ({}) -> no token (returnCode={}, msg={})", a.label, a.endpoint,
                rsp.has("returnCode") ? rsp.get("returnCode") : "?", msgOf(rsp));
        return false;
    }

    private LoginAttempt[] emailAttempts() {
        // The first entry is the confirmed CatLink email-login endpoint (POST login/email);
        // the rest remain as fallbacks in case the cloud changes.
        return new LoginAttempt[] { //
                new LoginAttempt("email-endpoint", CatlinkBindingConstants.EP_LOGIN_EMAIL,
                        loginParams(Map.of("email", email))),
                new LoginAttempt("email-as-mobile", CatlinkBindingConstants.EP_LOGIN,
                        loginParams(Map.of("mobile", email))),
                new LoginAttempt("email-param", CatlinkBindingConstants.EP_LOGIN, loginParams(Map.of("email", email))),
                new LoginAttempt("account-param", CatlinkBindingConstants.EP_LOGIN,
                        loginParams(Map.of("account", email))),
                new LoginAttempt("mailLogin-endpoint", "login/mailLogin", loginParams(Map.of("email", email))) };
    }

    private Map<String, String> loginParams(Map<String, String> identity) {
        Map<String, String> m = new TreeMap<>();
        m.put("platform", "ANDROID");
        m.put("password", encryptedPassword);
        m.putAll(identity);
        return m;
    }

    private boolean extractToken(JsonObject rsp) {
        JsonElement data = rsp.get("data");
        if (data != null && data.isJsonObject()) {
            JsonElement tok = data.getAsJsonObject().get("token");
            if (tok != null && !tok.isJsonNull() && !tok.getAsString().isEmpty()) {
                token = tok.getAsString();
                return true;
            }
        }
        return false;
    }

    private static String msgOf(JsonObject rsp) {
        for (String k : new String[] { "msg", "message", "returnMessage" }) {
            JsonElement e = rsp.get(k);
            if (e != null && !e.isJsonNull()) {
                return e.getAsString();
            }
        }
        return "";
    }

    /** One candidate email-login request (endpoint + params). */
    private static final class LoginAttempt {
        final String label;
        final String endpoint;
        final Map<String, String> params;

        LoginAttempt(String label, String endpoint, Map<String, String> params) {
            this.label = label;
            this.endpoint = endpoint;
            this.params = params;
        }
    }

    public boolean isLoggedIn() {
        return !token.isEmpty();
    }

    private void ensureLogin() throws CatlinkException {
        if (token.isEmpty()) {
            login();
        }
    }

    // ---- Generic helpers used by the device handlers ----------------------------------------

    /** GET the endpoint and return the {@code data} object (empty if absent). */
    public JsonObject getData(String api, Map<String, String> params) throws CatlinkException {
        ensureLogin();
        JsonObject rsp = request(api, new TreeMap<>(params), HttpMethod.GET, true);
        JsonElement data = rsp.get("data");
        return data != null && data.isJsonObject() ? data.getAsJsonObject() : new JsonObject();
    }

    /** GET a single-param ({@code deviceId}) endpoint and return {@code data.deviceInfo}, or {@code data}. */
    public JsonObject getDeviceInfo(String api, String deviceId) throws CatlinkException {
        JsonObject data = getData(api, mapOf("deviceId", deviceId));
        JsonElement info = data.get("deviceInfo");
        if (info != null && info.isJsonObject()) {
            return info.getAsJsonObject();
        }
        return data;
    }

    /** GET an endpoint and return the {@code data.<key>} array (empty if absent). */
    public JsonArray getDataArray(String api, Map<String, String> params, String key) throws CatlinkException {
        JsonObject data = getData(api, params);
        JsonElement arr = data.get(key);
        return arr != null && arr.isJsonArray() ? arr.getAsJsonArray() : new JsonArray();
    }

    /** POST a command; returns true when {@code returnCode == 0}. */
    public boolean post(String api, Map<String, String> params) throws CatlinkException {
        ensureLogin();
        return ok(request(api, new TreeMap<>(params), HttpMethod.POST, true));
    }

    // ---- Account / discovery ----------------------------------------------------------------

    /** Return all devices visible to the account. */
    public List<CatlinkDeviceSummary> getDevices() throws CatlinkException {
        ensureLogin();
        JsonObject rsp = request(CatlinkBindingConstants.EP_DEVICE_LIST, mapOf("type", "NONE"), HttpMethod.GET, true);
        List<CatlinkDeviceSummary> out = new ArrayList<>();
        JsonElement data = rsp.get("data");
        if (data != null && data.isJsonObject()) {
            JsonElement devs = data.getAsJsonObject().get("devices");
            if (devs != null && devs.isJsonArray()) {
                for (JsonElement el : devs.getAsJsonArray()) {
                    JsonObject o = el.getAsJsonObject();
                    out.add(new CatlinkDeviceSummary(asString(o, "id"), asString(o, "deviceName"), asString(o, "model"),
                            asString(o, "deviceType"), asString(o, "mac")));
                }
            }
        }
        return out;
    }

    /** Return the raw cat objects from the per-cat health endpoint. */
    public JsonArray getCats() throws CatlinkException {
        // The cats endpoint requires timezoneId; without it the cloud returns returnCode 1000.
        return getDataArray(CatlinkBindingConstants.EP_PET_CATS, Map.of("timezoneId", timezoneId()), "cats");
    }

    /** Return the daily health summary for one cat ({@code data} object, empty if absent). */
    public JsonObject getCatSummary(String petId, String dateIso) throws CatlinkException {
        Map<String, String> pms = new TreeMap<>();
        pms.put("petId", petId);
        pms.put("date", dateIso);
        pms.put("sport", "1");
        pms.put("timezoneId", timezoneId());
        return getData(CatlinkBindingConstants.EP_PET_SUMMARY, pms);
    }

    private static String timezoneId() {
        return java.time.ZoneId.systemDefault().getId();
    }

    // ---- Generic detail used by the legacy scooper handler ----------------------------------

    /** Return the {@code deviceInfo} object for a device via {@code token/device/info}. */
    public JsonObject getDeviceDetail(String deviceId) throws CatlinkException {
        return getDeviceInfo(CatlinkBindingConstants.EP_DEVICE_INFO, deviceId);
    }

    // ---- Helpers ----------------------------------------------------------------------------

    private static boolean ok(JsonObject rsp) {
        return !rsp.has("returnCode") || rsp.get("returnCode").getAsInt() == 0;
    }

    private static Map<String, String> mapOf(String k, String v) {
        Map<String, String> m = new TreeMap<>();
        m.put(k, v);
        return m;
    }

    private static String asString(JsonObject o, String key) {
        JsonElement el = o.get(key);
        return el == null || el.isJsonNull() ? "" : el.getAsString();
    }
}
