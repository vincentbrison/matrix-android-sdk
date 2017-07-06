/*
 * Copyright 2016 OpenMarket Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.androidsdk;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.matrix.androidsdk.rest.model.login.Credentials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents how to connect to a specific Homeserver, may include credentials to use.
 */
public class HomeserverConnectionConfig {
    private static final String CERTIFICATE_PINS_JSON_KEY = "certificate_pins";
    private Uri mHsUri;
    private Uri mIdentityServerUri;
    private Credentials mCredentials;
    private final List<CertificatePin> mCertificatePins = new ArrayList<>();

    /**
     * @param hsUri The URI to use to connect to the homeserver
     */
    public HomeserverConnectionConfig(Uri hsUri) {
        this(hsUri, null);
    }

    /**
     * @param hsUri The URI to use to connect to the homeserver
     * @param credentials The credentials to use, if needed. Can be null.
     */
    public HomeserverConnectionConfig(Uri hsUri, Credentials credentials) {
        this(hsUri, null, credentials, Collections.EMPTY_LIST);
    }

    /**
     * @param hsUri The URI to use to connect to the homeserver
     * @param identityServerUri The URI to use to manage identity
     * @param credentials The credentials to use, if needed. Can be null.
     * @param allowedFingerprints (deprecated) use certificatePins instead.
     * @param pin (deprecated) If true only allow certs matching given fingerprints, otherwise
     *            fallback to standard X509 checks.
     * @param certificatePins See https://square.github.io/okhttp/3.x/okhttp/okhttp3/CertificatePinner.html.
     */
    public HomeserverConnectionConfig(Uri hsUri, Uri identityServerUri, Credentials credentials, List<CertificatePin> certificatePins) {
        if (hsUri == null || (!"http".equals(hsUri.getScheme()) && !"https".equals(hsUri.getScheme())) ) {
            throw new RuntimeException("Invalid home server URI: "+hsUri);
        }

        if ((null != identityServerUri) && (!"http".equals(hsUri.getScheme()) && !"https".equals(hsUri.getScheme()))) {
            throw new RuntimeException("Invalid identity server URI: " + identityServerUri);
        }

        // remove trailing /
        if (hsUri.toString().endsWith("/")) {
            try {
                String url = hsUri.toString();
                hsUri = Uri.parse(url.substring(0, url.length()-1));
            } catch (Exception e) {
                throw new RuntimeException("Invalid home server URI: "+hsUri);
            }
        }

        // remove trailing /
        if ((null != identityServerUri) && identityServerUri.toString().endsWith("/")) {
            try {
                String url = identityServerUri.toString();
                identityServerUri = Uri.parse(url.substring(0, url.length()-1));
            } catch (Exception e) {
                throw new RuntimeException("Invalid identity server URI: " + identityServerUri);
            }
        }

        this.mHsUri = hsUri;
        this.mIdentityServerUri = identityServerUri;

        this.mCredentials = credentials;
        if (certificatePins != null) {
            this.mCertificatePins.addAll(certificatePins);
        }
    }

    public void setHomeserverUri(Uri uri) {
        mHsUri = uri;
    }
    public Uri getHomeserverUri() { return mHsUri; }

    public void setIdentityServerUri(Uri uri) {
        mIdentityServerUri = uri;
    }
    public Uri getIdentityServerUri() { return (null == mIdentityServerUri) ? mHsUri : mIdentityServerUri; }

    public Credentials getCredentials() { return mCredentials; }
    public void setCredentials(Credentials credentials) { this.mCredentials = credentials; }

    public List<CertificatePin> getCertificatePins() {
        return mCertificatePins;
    }

    @Override
    public String toString() {
        return "HomeserverConnectionConfig{" +
                "mHsUri=" + mHsUri +
                "mIdentityServerUri=" + mIdentityServerUri +
                ", mCredentials=" + mCredentials +
                ", certificatePins= " + mCertificatePins +
                '}';
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("home_server_url", mHsUri.toString());
        json.put("identity_server_url", getIdentityServerUri().toString());

        if (mCredentials != null) json.put("credentials", mCredentials.toJson());
        List<JSONObject> jsonCertificatePins = new ArrayList<>();
        for (CertificatePin certificatePin : mCertificatePins) {
            jsonCertificatePins.add(certificatePin.toJson());
        }
        json.put(CERTIFICATE_PINS_JSON_KEY, new JSONArray(jsonCertificatePins));

        return json;
    }

    public static HomeserverConnectionConfig fromJson(JSONObject obj) throws JSONException {
        List<CertificatePin> certificatePins = new ArrayList<>();
        if (obj.has(CERTIFICATE_PINS_JSON_KEY)) {
            JSONArray jsonCertificatePins = obj.getJSONArray(CERTIFICATE_PINS_JSON_KEY);
            for (int i = 0; i < jsonCertificatePins.length(); i++) {
                certificatePins.add(CertificatePin.fromJson(jsonCertificatePins.getJSONObject(i)));
            }
        }
        JSONObject credentialsObj = obj.optJSONObject("credentials");
        Credentials creds = credentialsObj != null ? Credentials.fromJson(credentialsObj) : null;
        HomeserverConnectionConfig config = new HomeserverConnectionConfig(
                Uri.parse(obj.getString("home_server_url")),
                obj.has("identity_server_url") ? Uri.parse(obj.getString("identity_server_url")) : null,
                creds,
                certificatePins
        );

        return config;
    }

    public static final class CertificatePin {
        private static final String HOSTNAME_JSON_KEY = "hostname";
        private static final String PUBLIC_HASH_KEY_JSON_KEY = "publicHashKey";
        private final String hostname;
        private final String publicKeyHash;

        public CertificatePin(String hostname, String publicKeyHash) {
            this.hostname = hostname;
            this.publicKeyHash = publicKeyHash;
        }

        public String getHostname() {
            return hostname;
        }

        public String getPublicKeyHash() {
            return publicKeyHash;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put(HOSTNAME_JSON_KEY, hostname);
            json.put(PUBLIC_HASH_KEY_JSON_KEY, publicKeyHash);
            return json;
        }

        public static CertificatePin fromJson(JSONObject json) throws JSONException {
            return new CertificatePin(
                json.getString(HOSTNAME_JSON_KEY),
                json.getString(PUBLIC_HASH_KEY_JSON_KEY)
            );
        }
    }
}
