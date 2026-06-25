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

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Cipher;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.catlink.internal.CatlinkBindingConstants;

/**
 * Reproduces the CatLink app's request signing and password encryption.
 *
 * <ul>
 * <li>Sign: sort params by key, join {@code k=v} with '&amp;', append
 * {@code &key=<SIGN_KEY>}, MD5, upper-case hex.</li>
 * <li>Password: md5(pwd) lower-hex &rarr; sha1 upper-hex &rarr; RSA/PKCS1v15
 * with the embedded public key &rarr; base64.</li>
 * </ul>
 *
 * @author Kornel Kovacs - Initial contribution
 */
@NonNullByDefault
public final class CatlinkCrypto {

    private CatlinkCrypto() {
    }

    public static String md5HexLower(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest); // lower-case
        } catch (Exception e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    /**
     * Compute the {@code sign} parameter over the (raw, unencoded) request params.
     * The supplied map must already contain {@code noncestr} and, if present, {@code token}.
     */
    public static String sign(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('&');
        }
        sb.append("key=").append(CatlinkBindingConstants.SIGN_KEY);
        return md5HexLower(sb.toString()).toUpperCase(Locale.ROOT);
    }

    /**
     * Encrypt a plain-text password the way the app does.
     */
    public static String encryptPassword(String plain, String rsaPublicKeyB64) {
        try {
            String md5 = md5HexLower(plain);
            byte[] sha = MessageDigest.getInstance("SHA-1").digest(md5.getBytes(StandardCharsets.UTF_8));
            String sha1 = HexFormat.of().formatHex(sha).toUpperCase(Locale.ROOT);

            PublicKey pub = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(rsaPublicKeyB64)));
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, pub);
            return Base64.getEncoder().encodeToString(cipher.doFinal(sha1.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Password encryption failed", e);
        }
    }
}
