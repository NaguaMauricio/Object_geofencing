package com.example.geofencing;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoQueryEventListener {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentUser;
    private DatabaseReference myLocationRef;
    private GeoFire geoFire;
    private List<LatLng> zonaArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                                  @Override
                                  public void onPermissionGranted(PermissionGrantedResponse response) {
                                      // Obtain the SupportMapFragment and get notified when the map is ready to be used.

                                      buildLocationRequest();
                                      buildLocationCallBack();
                                      fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(MapsActivity.this);

                                      SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                              .findFragmentById(R.id.map);
                                      mapFragment.getMapAsync(MapsActivity.this);

                                      initAreas();
                                      settingGeoFire();

                                  }


                    @Override
                                  public void onPermissionDenied(PermissionDeniedResponse response) {
                                      Toast.makeText(MapsActivity.this,"Es necesario dar los permisos correspondientes para utilizar la app",Toast.LENGTH_LONG).show();

                                  }

                                  @Override
                                  public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                                  }
                              }).check();


    }

    private void initAreas() {
        zonaArea= new ArrayList<>();
        zonaArea.add(new LatLng(-1.012015, -79.471875));
        //zonaArea.add(new LatLng(-1.013335, -79.471779));
        //zonaArea.add(new LatLng(-1.013528, -79.467155));
        FirebaseDatabase.getInstance().getReference("Aréa de peligro")
                .push()
                .setValue(zonaArea)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                       Toast.makeText(MapsActivity.this,"Actualizando!",Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MapsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void settingGeoFire() {
        myLocationRef= FirebaseDatabase.getInstance().getReference("Mi Localización");
        geoFire= new GeoFire(myLocationRef);

    }

    private void buildLocationCallBack() {
        locationCallback= new LocationCallback(){
            @Override
            public void onLocationResult(final LocationResult locationResult){
                if(mMap!=null){

                    geoFire.setLocation("TU", new GeoLocation(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if(currentUser!=null) currentUser.remove();

                            currentUser=mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(
                                            locationResult.getLastLocation().getLatitude(),
                                            locationResult.getLastLocation().getLongitude()))
                                    .title("tu"));
                            //AÑadir el marcador de posicion y mover la camara hacia esa posicon
                            mMap.animateCamera(CameraUpdateFactory
                                    .newLatLngZoom(currentUser.getPosition(),12.0f));
                        }
                    });



                }

            }

        };

    }

    private void buildLocationRequest() {
        locationRequest= new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);


        if(fusedLocationProviderClient!=null)
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                    return;
            }

            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
        //Añadir Circulo para marcar el area
        for(LatLng latLng :zonaArea)
        {
            //100 metros a la redonda
            mMap.addCircle(new CircleOptions().center(latLng)
            .radius(500).strokeColor(Color.GREEN)

            .fillColor(0x220000FF)
            .strokeWidth(5.0f));

            //Crear geoquery cuando el DISPOSITIVO SE ENCUENTRE DENTRO DE LA ZONA PERMITIDA
            GeoQuery geoQuery= geoFire.queryAtLocation(new GeoLocation(latLng.latitude,latLng.longitude),0.5f);//100metros
            geoQuery.addGeoQueryEventListener(MapsActivity.this);// 100 mts
        }



        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    protected void onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotificaction("Notification",String.format("%s El Objeto se encuentra dentro del aréa Segura",key));
    }

    @Override
    public void onKeyExited(String key) {
        sendNotificaction("Notification",String.format("%s El objeto se encuentra en una aréa peligrosa",key));

    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotificaction("Notification",String.format("%s El Objeto está llegando al aréa Segura",key));

    }



    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this,""+ error.getMessage(),Toast.LENGTH_LONG).show();


    }


    private void sendNotificaction(String xxxx, String format) {

        Toast.makeText(this,format,Toast.LENGTH_LONG).show();


        String NOTIFICACTION_CHANNEL_ID="edmt_multiple_location";
        NotificationManager notificationManager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICACTION_CHANNEL_ID,"mi notificación",
                    NotificationManager.IMPORTANCE_DEFAULT);

            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN);

            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
           //COMENTARIO
            // notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,NOTIFICACTION_CHANNEL_ID);
        builder.setContentTitle(xxxx)
        .setContentText(format)
        .setAutoCancel(false)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification= builder.build();
        notificationManager.notify(new Random().nextInt(),notification);

    }
}
