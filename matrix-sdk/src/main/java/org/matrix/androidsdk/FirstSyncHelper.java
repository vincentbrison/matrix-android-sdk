package org.matrix.androidsdk;

import android.support.annotation.Nullable;

import java.util.concurrent.CountDownLatch;

public class FirstSyncHelper {

    private static FirstSyncHelper instance = new FirstSyncHelper();

    private CountDownLatch firstSyncLatch = new CountDownLatch(0);
    private byte[] cachedFirstResponse;

    public static FirstSyncHelper getInstance() {
        return instance;
    }

    private FirstSyncHelper() {

    }

    public CountDownLatch getFirstSyncLatch() {
        return firstSyncLatch;
    }

    public void requestFirstSyncLatch() {
        if (firstSyncLatch.getCount() > 0) {
            firstSyncLatch.countDown();
        }
        firstSyncLatch = new CountDownLatch(1);
    }

    @Nullable
    public byte[] consumeFirstSyncResponse() {
        byte[] response = cachedFirstResponse;
        cachedFirstResponse = null;
        return response;
    }

    void setCachedFirstResponse(byte[] response) {
        cachedFirstResponse = response;
    }
}
