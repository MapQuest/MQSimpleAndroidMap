package com.mapquest.mapping.example;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapquest.android.commoncore.log.L;
import com.mapquest.android.searchahead.IllegalQueryParameterException;
import com.mapquest.android.searchahead.SearchAheadService;
import com.mapquest.android.searchahead.model.SearchAheadQuery;
import com.mapquest.android.searchahead.model.SearchCollection;
import com.mapquest.android.searchahead.model.response.AddressProperties;
import com.mapquest.android.searchahead.model.response.Place;
import com.mapquest.android.searchahead.model.response.SearchAheadResponse;
import com.mapquest.android.searchahead.model.response.SearchAheadResult;
import com.mapquest.mapping.MapQuest;
import com.mapquest.mapping.maps.MapView;
import com.mapquest.mapping.maps.MapboxMapTrafficPresenter;
import com.mapquest.mapping.maps.MyLocationPresenter;
import com.mapquest.mapping.utils.PoiOnMapData;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final float DEFAULT_ZOOM_LEVEL = 12;
    private static final LatLng MAPQUEST_HEADQUARTERS_LOCATION = new LatLng(39.750307, -104.999472);
    private static final double FOLLOW_MODE_TILT_VALUE_DEGREES = 50;
    private static final double CENTER_ON_USER_ZOOM_LEVEL = 18;
    private MapView mMapView;
    private MapboxMap mMapboxMap;
    private MapboxMapTrafficPresenter mMapboxMapTrafficPresenter;
    private boolean mTrafficFlag;
    private MyLocationPresenter locationPresenter;
    private FusedLocationProviderClient mFusedLocationClient;
    private PermissionsManager permissionsManager;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private SearchAheadService mSearchAheadServiceV3;
    private List<Marker> mPOIMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapQuest.start(getApplicationContext());
        setContentView(R.layout.activity_main);

        mTrafficFlag = false;

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        mMapView = findViewById(R.id.mapquestMapView);

        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(mapboxMap -> {
            mMapboxMap = mapboxMap;
            mMapboxMapTrafficPresenter = new MapboxMapTrafficPresenter(mMapboxMap, mMapView);

            mMapView.setPOIOnMapSelectedListener(poiOnMapList -> {
                PoiOnMapData selectedPOI = poiOnMapList.get(0);
                showMarkerForPOI(selectedPOI);
            });

//            // This is the way to set style changes to your map on style load
//            mMapView.addOnMapChangedListener(change -> {
//                if (change == DID_FINISH_LOADING_STYLE) {
//                    FillLayer fillLayer = (FillLayer) mMapboxMap.getLayer("building");
//                    if (fillLayer != null) {
//                        fillLayer.setProperties(
//                                PropertyFactory.fillColor(Color.RED)
//                        );
//                    }
//                }
//            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.street_view:
                mMapView.setStreetMode();
                return true;
            case R.id.satellite_view:
                mMapView.setSatelliteMode();
                return true;
            case R.id.night_view:
                mMapView.setNightMode();
                return true;
            case R.id.add_polygon:
                setPolygon(mMapboxMap);
                return true;
            case R.id.add_polyline:
                setPolyline(mMapboxMap);
                return true;
            case R.id.add_annotation:
                addMarker(mMapboxMap);
                return true;
            case R.id.goto_Denver:
                initializeMapView(mMapView, mMapboxMap);
                return true;
            case R.id.goto_user_loc:
                enableUserTracking(mMapView, mMapboxMap);
                return true;
            case R.id.toggle_traffic:
                if (mTrafficFlag) {
                    mMapboxMapTrafficPresenter.setTrafficFlowLayerOff();
                    mMapboxMapTrafficPresenter.setTrafficIncidentLayerOff();
                } else {
                    mMapboxMapTrafficPresenter.setTrafficFlowLayerOn();
                    mMapboxMapTrafficPresenter.setTrafficIncidentLayerOn();
                }
                mTrafficFlag = !mTrafficFlag;
                return true;
            case R.id.styleBuildings:
                setBuildingsRed(mMapboxMap);
                return true;

            case R.id.add_coffeeshops:
                searchCoffeeShops();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showMarkerForPOI(PoiOnMapData selectedPOI) {
        //Make a transparent marker
        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        Bitmap transparent = Bitmap.createBitmap(1, 1, conf); // this creates a transparent bitmap

        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(MBLatLngUtil.fromMQLatLng(selectedPOI.getLatLng()));
        markerOptions.title(selectedPOI.getName());
        markerOptions.setIcon(iconFactory.fromBitmap(transparent));

        for (Marker marker : mPOIMarkers) {
            mMapboxMap.removeMarker(marker);
        }

        Marker marker = mMapboxMap.addMarker(markerOptions);
        mPOIMarkers.add(marker);
        mMapboxMap.selectMarker(marker);
    }

    private void setBuildingsRed(MapboxMap mapboxMap) {
        FillLayer fillLayer = (FillLayer) mapboxMap.getLayer("building");
        if (fillLayer != null) {
            fillLayer.setProperties(
                    PropertyFactory.fillColor(Color.RED)
            );
        }
    }

    private void setPolyline(MapboxMap mapboxMap) {

        LatLng CAMERA_LOC = new LatLng(39.745391, -105.00653);
        mMapView.setStreetMode();
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CAMERA_LOC, 13));

        List<LatLng> coordinates = new ArrayList<>();

        coordinates.add(new LatLng(39.74335,-105.01234));
        coordinates.add(new LatLng(39.74667,-105.01135));
        coordinates.add(new LatLng(39.7468,-105.00709));
        coordinates.add(new LatLng(39.74391,-105.00794));
        coordinates.add(new LatLng(39.7425,-105.0047));
        coordinates.add(new LatLng(39.74634,-105.00478));
        coordinates.add(new LatLng(39.74734,-104.99984));

        PolylineOptions polyline = new PolylineOptions();
        polyline.addAll(coordinates);
        polyline.width(3);
        polyline.color(Color.BLUE);
        mapboxMap.addPolyline(polyline);
    }

    private void setPolygon(MapboxMap mapboxMap) {
        LatLng CAMERA_LOC = new LatLng(39.743943, -105.020089);
        mMapView.setStreetMode();
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CAMERA_LOC, 15));

        List<LatLng> coordinates = new ArrayList<>();
        coordinates.add(new LatLng(39.744465080845458,-105.02038957961648));
        coordinates.add(new LatLng(39.744460864711129,-105.01981090977684));
        coordinates.add(new LatLng(39.744379574636383,-105.01970518778262));
        coordinates.add(new LatLng(39.743502042120781,-105.01970874744497));
        coordinates.add(new LatLng(39.743419794549339,-105.01977839958302));
        coordinates.add(new LatLng(39.74341214360723,-105.02038412442006));
        coordinates.add(new LatLng(39.74349726029007,-105.02049233399056));
        coordinates.add(new LatLng(39.744393745651706,-105.0204836274754));

        PolygonOptions polygon = new PolygonOptions();
        polygon.addAll(coordinates);
        polygon.fillColor(Color.rgb(255, 102, 0));
        polygon.strokeColor(Color.BLACK);
        mapboxMap.addPolygon(polygon);
    }

    private void addMarker(MapboxMap mapboxMap) {
        initializeMapView(mMapView, mapboxMap);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(MAPQUEST_HEADQUARTERS_LOCATION);
        markerOptions.title("MapQuest");
        markerOptions.snippet("Welcome to Denver!");
        mapboxMap.addMarker(markerOptions);

        mapboxMap.setOnMarkerClickListener(marker -> {
            Toast.makeText(MainActivity.this, marker.getTitle(), Toast.LENGTH_LONG).show();
            return true;
        });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void initializeLocationEngine(Runnable success) {

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            success.run();
        } else {
            permissionsManager = new PermissionsManager(new PermissionsListener() {

                @Override
                public void onExplanationNeeded(List<String> permissionsToExplain) {
                    // Left blank on purpose
                }

                @Override
                public void onPermissionResult(boolean granted) {
                    if (granted) {
                        success.run();
                    }
                }

            });
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressLint("MissingPermission")
    private void setupLocationEngine(LocationCallback locationCallback) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (mLocationCallback != null && mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        } else if (mFusedLocationClient == null) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }

        mLocationCallback = locationCallback;
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
   }

   private void cleanupLocationEngine() {
       mFusedLocationClient.removeLocationUpdates(mLocationCallback);
       mLocationRequest = null;
       mLocationCallback = null;
   }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void enableUserTracking(MapView mapView, MapboxMap mapboxMap) {

        initializeLocationEngine(() -> {
            setupLocationPresenter(mapView, mapboxMap);
            setupLocationEngine( new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        locationPresenter.forceLocationChange(location);
                    }
                }

            });
        });
    }

    private void setupLocationPresenter(MapView mapView, MapboxMap mapboxMap) {

        if (locationPresenter == null) {
            locationPresenter = new MyLocationPresenter(mapView, mapboxMap, null);
        }

        locationPresenter.setInitialZoomLevel(CENTER_ON_USER_ZOOM_LEVEL);
        locationPresenter.setFollowCameraAngle(FOLLOW_MODE_TILT_VALUE_DEGREES);
        locationPresenter.setLockNorthUp(false);
        locationPresenter.setFollow(true);
        locationPresenter.onStart();
    }

    private void initializeMapView(MapView mapView, MapboxMap mapboxMap) {
        mapView.setStreetMode();
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MAPQUEST_HEADQUARTERS_LOCATION, DEFAULT_ZOOM_LEVEL));
    }

    private void searchCoffeeShops() {

        initializeLocationEngine(() -> {
            setupLocationEngine( new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }

                    cleanupLocationEngine();
                    performSearch("coffee", locationResult);
                }

            });
        });
    }

    private void performSearch(String queryString, LocationResult locationResult) {

        Location lastLocation = locationResult.getLastLocation();
        mMapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 10));

        if (mSearchAheadServiceV3 == null) {
            mSearchAheadServiceV3 = new SearchAheadService(this, BuildConfig.API_KEY);
        }

        List<SearchCollection> searchCollections = Collections.singletonList(SearchCollection.POI);
        try {
            MQLatLng latLng = new MQLatLng(lastLocation);

            SearchAheadQuery query = new SearchAheadQuery.Builder(queryString, searchCollections).location(latLng).build();
            mSearchAheadServiceV3.predictResultsFromQuery(query,
                    new SearchAheadService.SearchAheadResponseCallback() {

                        @Override
                        public void onSuccess(@NonNull SearchAheadResponse searchAheadResponse) {

                            MainActivity.this.runOnUiThread(() -> {
                                for (SearchAheadResult result : searchAheadResponse.getResults()) {
                                    MarkerOptions markerOptions = new MarkerOptions();
                                    Place place = result.getPlace();
                                    AddressProperties aProp = place.getAddressProperties();

                                    String address = new StringBuilder()
                                            .append(StringUtils.defaultIfEmpty(aProp.getStreet(), "")).append("\n")
                                            .append(StringUtils.defaultIfEmpty(aProp.getCity(), "")).append(" ")
                                            .append(StringUtils.defaultIfEmpty(aProp.getStateCode(), "")).append( " ")
                                            .append(StringUtils.defaultIfEmpty(aProp.getPostalCode(), "")).append("\n")
                                           .toString();
                                    markerOptions.position( MBLatLngUtil.fromMQLatLng( place.getLatLng() ) );
                                    markerOptions.title(result.getDisplayString());
                                    markerOptions.snippet(address);
                                    mMapboxMap.addMarker(markerOptions);
                                }
                            });
                         }

                        @Override
                        public void onError(Exception e) {
                            L.e("Search Ahead V3 Failure", e);
                        }
                    });
        } catch (IllegalQueryParameterException e) {
            L.e("Error performing search", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }

        if (locationPresenter != null) {
            locationPresenter.onStart();
        }
    }

    @Override
    public void onPause()  {
        super.onPause();
        mMapView.onPause();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        if (locationPresenter != null) {
            locationPresenter.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

}
