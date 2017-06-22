package com.connection_telecom.touchpoint;

import java.util.List;

/**
 * Created by michaeld on 2017-02-27.
 */
public interface ChannelAvailabilityCallback {
    void onSuccess(List<String> availableChannels, List<String> unavailableChannels);
    void onFailure(Throwable error);
}
