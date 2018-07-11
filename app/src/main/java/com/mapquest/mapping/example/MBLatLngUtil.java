package com.mapquest.mapping.example;

import com.mapbox.mapboxsdk.geometry.LatLng;

public final class MBLatLngUtil extends LatLng {

    static LatLng fromMQLatLng(com.mapquest.android.commoncore.model.LatLng latLng) {
        return new LatLng((double)latLng.getLatitude(), (double)latLng.getLongitude());
    }
}
