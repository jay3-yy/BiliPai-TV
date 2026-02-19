package com.android.purebilibili.feature.cast;

import com.android.purebilibili.feature.cast.CastDeviceInfo;

interface ICastBridgeService {
    void connect();
    void disconnect();
    boolean isConnected();
    void refresh();
    List<CastDeviceInfo> getDevices();
    boolean cast(String udn, String url, String title, String creator);
    boolean stop(String udn);
}
