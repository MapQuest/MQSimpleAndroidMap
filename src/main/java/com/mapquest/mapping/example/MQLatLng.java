package com.mapquest.mapping.example;

import android.location.Location;

public final class MQLatLng extends com.mapquest.android.commoncore.model.LatLng {

    public MQLatLng(Location location) {
        super((float)location.getLatitude(), (float)location.getLongitude());
     }

}
