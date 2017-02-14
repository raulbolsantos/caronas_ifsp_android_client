package projetocaronas.tcc.ifsp.br.projetocarona;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import projetocaronas.tcc.ifsp.br.projetocarona.entities.User;
import projetocaronas.tcc.ifsp.br.projetocarona.tasks.ConnectionReceiveJSONTask;
import projetocaronas.tcc.ifsp.br.projetocarona.tasks.ConnectionSendJSONTask;
import projetocaronas.tcc.ifsp.br.projetocarona.utils.AndroidUtilsCaronas;

import static android.content.Context.LOCATION_SERVICE;
import static android.graphics.Color.YELLOW;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ConnectionReceiveJSONTask.OnJsonTransmitionCompleted, LocationListener {
    private LatLng latLng = null;
    private GoogleMap mMap;
    private User userToRegister = null;
    private JSONArray usersData = null;
    private boolean gotUserPosition = false; //FIXME Precisa ser estático? Em quais situações será instanciada uma nova classe?
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtains user from UserRegisterActivity
        Intent intent = getIntent();
        userToRegister = (User) intent.getSerializableExtra("newUser");

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        // Forces map to MyLocation
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(bestProvider);
        if (location != null) {
            onLocationChanged(location);
        }
        locationManager.requestLocationUpdates(bestProvider, 20000, 0, this);


        // Click event of get current location
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        mMap.clear(); // Clear All Markers and other stuff

                        double longitude = cameraPosition.target.longitude;
                        double latitide = cameraPosition.target.latitude;

                        latLng = new LatLng(latitide, longitude);
                        mMap.addMarker(new MarkerOptions().position(latLng).title("Seu local"));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        mMap.setOnCameraChangeListener(null);//disable
                        fillAddress(latitide, longitude);
                        // Fill markers
                        populateMapWithUsers(usersData);
                    }
                });

                return false;
            }
        });
        // Fill map with data of users
        new ConnectionReceiveJSONTask(this, MapsActivity.this, "/getAllUsersAndPools").execute();

        // Event of click on Info Window each marker (User or Ride)
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Toast.makeText(MapsActivity.this, "User selected " + marker.getTitle().toString(), Toast.LENGTH_SHORT).show();//TODO  Call activity Confirma carona
            }
        });

    }

    public void onSearch(View view) {
        hideKeyboardFromUtils(view);

        EditText locationEditText = (EditText) findViewById(R.id.editTextSearch);
        String location = locationEditText.getText().toString();

        mMap.clear(); // Clear All Markers and other stuff

        if (location != null && !location.isEmpty()) {

            Geocoder geocoder = new Geocoder(this);
            try {
                List<Address> addressList = geocoder.getFromLocationName(location, 1);
                if (addressList.size() > 0) {
                    Address address = addressList.get(0);

                    latLng = new LatLng(address.getLatitude(), address.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(latLng).title("Seu local"));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                } else {
                    Toast.makeText(MapsActivity.this, "O endereço informado não foi encontrado", Toast.LENGTH_SHORT).show();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void hideKeyboardFromUtils(View view) {
        AndroidUtilsCaronas.hideKeyboard(this);
    }

    public void onSaveLocation(View view) {
        if (latLng != null) {

            JSONObject postParameters = this.userToRegister.toJSONObject();

            try {
                postParameters.put("latitude", latLng.latitude);
                postParameters.put("longitude", latLng.longitude);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Toast.makeText(this, "Cadastro realizado, localização : Latitude: " + latLng.latitude + " Longitude: " + latLng.longitude, Toast.LENGTH_LONG).show();
            // Envia os dados, chamando a prócima activity de cadastro de caronas
            new ConnectionSendJSONTask(MapsActivity.this, new RegisterRidesActivity(), "/register_user_and_coordinates").execute(postParameters);
        } else {
            Toast.makeText(this, "Cannot read location!", Toast.LENGTH_LONG).show();
        }
    }

    public void onBackButton(View view) {
        Intent intent = new Intent(this, UserRegisterActivity.class);
        intent.putExtra("userToRegister", userToRegister);
        startActivity(intent);
    }

    @Override
    public void onTrasmitionCompleted(JSONArray jsonArray) {
        // Prevenir null pointer exception
        if (jsonArray == null) jsonArray = new JSONArray();
        // Fill with location markers
        populateMapWithUsers(jsonArray);
        Toast.makeText(this, "Encontrados: " + jsonArray.length() + " usuários", Toast.LENGTH_LONG).show();
        usersData = jsonArray;
    }

    public void populateMapWithUsers(JSONArray usersData) {
        boolean canUserGiveRide = checkUserCanGiveRide();
        for (int i = 0; i < usersData.length(); i++) {
            try {
                JSONObject user = (JSONObject) usersData.get(i);

                boolean giveRide = (Boolean) user.get("canGiveRide");
//                int vacancy = (int) user.get("vacancy");
                int vacancy = 0; //FIXME Só para teste, pegar da carona do usuário (ou listar as caronas separado, tirando o campo vagas?)


                LatLng userLatLng = new LatLng((Double) user.getJSONObject("location").get("latitude"), (Double) user.getJSONObject("location").get("longitude"));
                if (giveRide) {
                    if (vacancy > 0) {
                        mMap.addMarker(new MarkerOptions().position(userLatLng).title(user.get("name").toString()).snippet("Vagas : " + vacancy + " - Pedir carona").icon(BitmapDescriptorFactory.fromResource(R.drawable.car_vacancy_marker)));
                    } else {
                        mMap.addMarker(new MarkerOptions().position(userLatLng).title(user.get("name").toString()).snippet("Lotado").icon(BitmapDescriptorFactory.fromResource(R.drawable.car_marker)));
                    }
                } else {
                    mMap.addMarker(new MarkerOptions().position(userLatLng).title(user.get("name").toString()).snippet(canUserGiveRide ? "Oferecer carona" : "Ver perfil").icon(BitmapDescriptorFactory.fromResource(R.drawable.man_marker)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }

    private boolean checkUserCanGiveRide() {
        if (this.userToRegister != null) {
            return this.userToRegister.isCanGiveRide();
        }
        return false;
    }

    @Override
    public void onProviderEnabled(String s) {
        // Para utilizar LocationListener
    }

    @Override
    public void onProviderDisabled(String s) {
        // Para utilizar LocationListener
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        // Para utilizar LocationListener
    }

    /**
     * Get the current user's current position
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        if(this.gotUserPosition == false) {
            this.gotUserPosition = true; // Run once
            // Sets marker on current postion
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng latLng = new LatLng(latitude, longitude);
            this.mMap.addMarker(new MarkerOptions().position(latLng)); //TODO Refactor this. Put in fillAddress Method too

            this.mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            this.mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            fillAddress(latitude, longitude);

        }
    }

    private void fillAddress(double latitude, double longitude) {
        try {
            // Fills the address information
            Geocoder geocoder = new Geocoder(this);
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            Address address = addressList.get(0);
            Toast.makeText(MapsActivity.this, "Current address - Street: " + address.getAddressLine(0), Toast.LENGTH_SHORT).show();
            EditText locationEditText = (EditText) findViewById(R.id.editTextSearch);
            locationEditText.setText(address.getAddressLine(0)+", "+ address.getLocality() +" - "+address.getAdminArea()+", "+address.getCountryName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Maps Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
