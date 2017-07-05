package org.matrix.androidsdk;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import org.matrix.androidsdk.rest.client.MXRestExecutorService;
import org.matrix.androidsdk.rest.model.login.Credentials;
import org.matrix.androidsdk.util.UnsentEventsManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.CertificatePinner;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class OkHttpClientProvider {
    private static final String PARAM_ACCESS_TOKEN = "access_token";
    private static final int CONNECTION_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 60000;
    private static final int WRITE_TIMEOUT_MS = 60000;
    private static final int DOWNLOAD_READ_TIME_OUT_MS = 10000;

    private static int restParametersHash = 0;
    private static OkHttpClient restOkHttpClient;
    private static int downloadParametersHash = 0;
    private static OkHttpClient downloadOkHttpClient;
    private static int uploadParametersHash = 0;
    private static OkHttpClient uploadOkHttpClient;


    private OkHttpClientProvider() {
    }

    public static synchronized OkHttpClient getRestOkHttpClient(
        Credentials credentials,
        UnsentEventsManager unsentEventsManager,
        HomeserverConnectionConfig hsConfig,
        boolean useMXExececutor
    ) {
        int parametersHash = computeParametersHash(
            credentials,
            unsentEventsManager,
            hsConfig,
            useMXExececutor
        );
        if (parametersHash != restParametersHash) {
            restParametersHash = parametersHash;
            initRestOkHttpClient(
                credentials,
                unsentEventsManager,
                hsConfig,
                useMXExececutor
            );
        }
        return restOkHttpClient;
    }

    public static synchronized OkHttpClient getDownloadOkHttpClient(
        HomeserverConnectionConfig hsConfig
    ) {
        int parameterHash = computeParametersHash(hsConfig);
        if (parameterHash != downloadParametersHash) {
            downloadParametersHash = parameterHash;
            initDownloadOkHttpClient(hsConfig);
        }
        return downloadOkHttpClient;
    }

    public static synchronized OkHttpClient getUploadOkHttpClient() {
        if (uploadOkHttpClient == null) {
            initUploadOkHttpClient();
        }
        return uploadOkHttpClient;
    }

    private static int computeParametersHash(Object... values) {
        int result = 0;
        for (Object object : values) {
            result = 37 * result + (object == null ? 0 : object.hashCode());
        }
        return result;
    }

    private static void initRestOkHttpClient(
        Credentials credentials,
        UnsentEventsManager unsentEventsManager,
        HomeserverConnectionConfig hsConfig,
        boolean useMXExececutor
    ) {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient()
            .newBuilder()
            .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addInterceptor(makeAuthenticationInterceptor(
                UserAgentProvider.getUserAgent(),
                credentials
            ))
            .addInterceptor(makeConnectivityInterceptor(unsentEventsManager))
            .addNetworkInterceptor(new StethoInterceptor());
        configureCertificatPinning(hsConfig, okHttpClientBuilder);
        if (useMXExececutor) {
            okHttpClientBuilder.dispatcher(new Dispatcher(new MXRestExecutorService()));
        }
        restOkHttpClient = okHttpClientBuilder.build();
    }

    private static void initDownloadOkHttpClient(HomeserverConnectionConfig hsConfig) {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient()
            .newBuilder()
            .readTimeout(DOWNLOAD_READ_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        configureCertificatPinning(hsConfig, okHttpClientBuilder);
        downloadOkHttpClient = okHttpClientBuilder.build();
    }

    private static void initUploadOkHttpClient() {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient()
            .newBuilder()
            .cache(null);
        uploadOkHttpClient = okHttpClientBuilder.build();
    }

    private static void configureCertificatPinning(
        HomeserverConnectionConfig hsConfig,
        OkHttpClient.Builder okHttpClientBuilder
    ) {
        List<HomeserverConnectionConfig.CertificatePin> certificatePins =
            hsConfig.getCertificatePins();
        if (!certificatePins.isEmpty()) {
            CertificatePinner.Builder builder = new CertificatePinner.Builder();
            for (HomeserverConnectionConfig.CertificatePin certificatePin : certificatePins) {
                builder.add(certificatePin.getHostname(), certificatePin.getPublicKeyHash());
            }
            okHttpClientBuilder.certificatePinner(builder.build());
        }
    }

    private static Interceptor makeAuthenticationInterceptor(
        final String userAgent, final Credentials credentials
    ) {
        return new Interceptor() {
            @Override public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Request.Builder newRequestBuilder = request.newBuilder();
                if (null != userAgent) {
                    // set a custom user agent
                    newRequestBuilder.addHeader("User-Agent", userAgent);
                }

                // Add the access token to all requests if it is set
                if ((credentials != null) && (credentials.accessToken != null)) {
                    HttpUrl url = request
                        .url()
                        .newBuilder()
                        .addEncodedQueryParameter(
                            PARAM_ACCESS_TOKEN,
                            credentials.accessToken
                        ).build();
                    newRequestBuilder.url(url);
                }

                request = newRequestBuilder.build();

                return chain.proceed(request);
            }
        };
    }

    private static Interceptor makeConnectivityInterceptor(
        final UnsentEventsManager unsentEventsManager
    ) {
        return new Interceptor() {
            @Override public Response intercept(Chain chain) throws IOException {
                if (unsentEventsManager != null
                    && unsentEventsManager.getNetworkConnectivityReceiver() != null
                    && !unsentEventsManager.getNetworkConnectivityReceiver().isConnected()) {
                    throw new IOException("Not connected");
                }
                return chain.proceed(chain.request());
            }
        };
    }
}
