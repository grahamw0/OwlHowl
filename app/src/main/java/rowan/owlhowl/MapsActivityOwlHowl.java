package rowan.owlhowl;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Map;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Main class. When the splash screen is finished
 * the onCreate of this class gets initiated.
 * Sets up and interacts with the map.
 *
 * @author Ryan Godfrey, Adam, Leif, Will, Brandon, Cullen
 * @version 1.awesome
 */

public class MapsActivityOwlHowl extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap; // creates the map
    private final static int MY_PERMISSIONS_FINE_LOCATION = 101;
    ZoomControls zoom;
    Button markBt;
    Button clear;
    Button post;
    Button getMessages;
    Button getLoc;
    Button clearsaved;
    static final int REQUEST_LOCATION = 1;
    LocationManager locationManager;
    LatLng myLocation;
    Circle circle;
    List<Marker> mMarkers = new ArrayList<Marker>();
    List<LatLng> savedLocations = new ArrayList<LatLng>();
    List<String> savedLocs = new ArrayList<String>();
    JSONArray howls = new JSONArray();
    static final String ID_FILE = "id_file";
    String identifier = null;


    // This is the constructor. Sets the map up.  This is called first.  When the
    // screen is tilted it will start here by setting up
    // the map again.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_owl_howl);

        try{
            //try to read from static final file location
            FileInputStream fis =openFileInput(ID_FILE);
            //convert bytes into string
            StringBuffer sb = new StringBuffer("");
            int next;
            while((next = fis.read()) != -1){
                sb.append((char) next);
            }
            fis.close();
            //assign that string to field for use
            identifier = sb.toString();
        }catch (IOException e){
            //If the file is not found, the app is being open for the first time
            //This block will generate a UUID and write it to ID_FILE location
            identifier = UUID.randomUUID().toString();
            try {
                FileOutputStream fos = openFileOutput(ID_FILE, Context.MODE_PRIVATE);
                fos.write(identifier.getBytes());
                fos.close();
            }catch (Exception e1){
                Toast.makeText(MapsActivityOwlHowl.this, "File write error", Toast.LENGTH_SHORT).show();
            }
        }

        new rowan.owlhowl.List();
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // "Get Howls" button.  Sets the on click listener.
        getMessages = (Button) findViewById(R.id.btGetMes);
        getMessages.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //update howls.  Initiates the Get Request to the database
                new SendGetRequest().execute(getLocation().latitude, getLocation().longitude);
            }
        });


        //Creates the zoom in and out environment
        zoom = (ZoomControls) findViewById(R.id.zcZoom);
        zoom.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        zoom.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });


        // My Location button places a marker on the map from finger click.
        markBt = (Button) findViewById(R.id.btMark);
        markBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                LatLng myLocation = getLocation();
                // create a custom marker
                MarkerOptions options2 = new MarkerOptions()
                        .position(myLocation)
                        .title("POST an anonymous HOWL at the top,")
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.profhead))
                        .snippet("or press the Get HOWLS button below.");
                Marker marker2 = mMap.addMarker(options2);
                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                    @Override
                    public View getInfoWindow(Marker marker) {
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {
                        View v = getLayoutInflater().inflate(R.layout.info_window, null);
                        TextView tvLocality = (TextView) v.findViewById(R.id.tv_locality);
                        TextView tvSnippet = (TextView) v.findViewById(R.id.tv_Snippet);
                        TextView tvRaius = (TextView) v.findViewById(R.id.tv_radius_description);
                        TextView tvLat = (TextView) v.findViewById(R.id.tv_lat);
                        TextView tvLong = (TextView) v.findViewById(R.id.tv_long);

                        LatLng ll = marker.getPosition();
                        tvLocality.setText(marker.getTitle());
                        tvSnippet.setText(marker.getSnippet());
                        tvRaius.setText("HOWL range = 2.5 miles");
                        tvLat.setText("Latitude: " + ll.latitude);
                        tvLong.setText("Longitude: " + ll.longitude);

                        return v;
                    }
                });
                marker2.showInfoWindow();
                // add the maker with the following options
                mMarkers.add(marker2);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 12.0f));

                drawCircle(myLocation);
            }
        });


        // Post button gets the text form the main textView and
        // sends it to the database.
        post = (Button) findViewById(R.id.btPost);
        post.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText postText = (EditText) findViewById(R.id.etLocationEntry);
                String pos = postText.getText().toString();
                if (!pos.equals("")) {
                    new SendPostRequest().execute(pos);
                }
                postText.setText("");

                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });


        // Clear button calls the method removeMarkers()
        // We had to add the markers to an arrayList so that
        // the circle would stay on the screen when we cleared the
        // map. The method removeMarkers() clears the ArrayList so the
        // markers disappear.
        clear = (Button) findViewById(R.id.btClear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeMarkers();

            }
        });


        // calls the locationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        // Get Saved Locations button
        getLoc = (Button) findViewById(R.id.getLocation);
        getLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeMarkers();
                LatLng latLng = getLocation();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8.0f));
                getSavedLocations();
            }

        });


        // Clear Saved Locations button

        clearsaved = (Button) findViewById(R.id.clearSaved);
        clearsaved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear both ArrayLists and the map
                savedLocations.clear();
                savedLocs.clear();
                mMap.clear();


                // Clear SharedPerferences data
                SharedPreferences settings = getSharedPreferences("PREFS", 0);
                SharedPreferences.Editor editor = settings.edit().clear();

                editor.commit();
            }
        });
    }
    // ******************   End onCreate() Area *********************

    //*********** Beggining of On Map Ready Area ***********************************

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
        // Get current location and focus upon start up
        final LatLng myLocation = getLocation();
        // move the camera to that location and zoom in
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 12.0f));
        // Draw the circle that surrounds that location
        circle = drawCircle(myLocation);

        // upon start up read from the sharedPerferences String
        // and re-populate the ArrayList<Latlng> with the saved location

            SharedPreferences settings = getSharedPreferences("PREFS", 0);
            String savedLocsString = settings.getString("savedLocs", "");
        // If what is stored in sharedPerferences is not empty then
        // convert it back to Doubles to create the Latlng Objects and
        // reload the ArrayList<Latlng>
        if(!savedLocsString.equals("")) {
            Set<LatLng> set = new HashSet<>();
            String[] itemsSavedLocs = savedLocsString.split(":");

            for (String pair : itemsSavedLocs) {
                double lat = Double.valueOf(pair.split(",")[0]);
                double lng = Double.valueOf(pair.split(",")[1]);
                savedLocations.add(new LatLng(lat, lng));

                // This wipes out the duplicate Latlng Objects that I am ending up with
                // from reading and writing to sharedPerferences.  Store them in a Hashset
                // then repopulate the ArrayList with no duplicates.
                set.addAll(savedLocations);
                savedLocations.clear();
                savedLocations.addAll(set);


            }
        }


        // onInfo window click listener.  THis saves the locations.
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                LatLng markerPosition = marker.getPosition();
                // If the marker position is NOT in the arrayList then add it
                if (!savedLocations.contains(markerPosition)){
                    savedLocations.add(markerPosition);
                    Toast.makeText(MapsActivityOwlHowl.this, "Location saved", Toast.LENGTH_SHORT).show();

                    // attempt at storing

                    for(LatLng ll : savedLocations){
                        String s = String.valueOf(ll.latitude + "," + ll.longitude);
                        savedLocs.add(s);

                    }

                    StringBuilder stringBuilder = new StringBuilder();
                    for(String s : savedLocs){
                        stringBuilder.append(s);
                        stringBuilder.append(":");
                    }
                    SharedPreferences settings = getSharedPreferences("PREFS", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("savedLocs", stringBuilder.toString());
                    editor.commit();

                    // If the marker is already in the list then get the messages
                } else {

                    Toast.makeText(MapsActivityOwlHowl.this, "send get request", Toast.LENGTH_SHORT).show();
                    //update howls.  Initiates the Get Request to the database
                    new SendGetRequest().execute(markerPosition.latitude, markerPosition.longitude);
                }

            }

        });

        mMap.setOnInfoWindowLongClickListener(new GoogleMap.OnInfoWindowLongClickListener() {
            @Override
            public void onInfoWindowLongClick(Marker marker) {

                Toast.makeText(MapsActivityOwlHowl.this, "Location Removed", Toast.LENGTH_SHORT).show();
                LatLng markerPosition = marker.getPosition();
                savedLocations.remove(markerPosition);
                savedLocs.clear();
                mMap.clear();
                drawCircle(myLocation);
                getSavedLocations();
                SharedPreferences settings = getSharedPreferences("PREFS", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.clear();
                editor.commit();

                for(LatLng ll : savedLocations){
                    String s = String.valueOf(ll.latitude + "," + ll.longitude);
                    savedLocs.add(s);

                }

                StringBuilder stringBuilder = new StringBuilder();
                for(String s : savedLocs){
                    stringBuilder.append(s);
                    stringBuilder.append(":");
                }

                editor.putString("savedLocs", stringBuilder.toString());
                editor.commit();

            }

        });

        // Create the marker that shows up on the user's location
        MarkerOptions options = new MarkerOptions()
                .position(myLocation)
                .title("'POST' an 'ANONYMOUS HOWL' at the top")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.profhead))
                .snippet("or press the 'Get HOWLS' button below.");
        Marker marker1 = mMap.addMarker(options);

        // Create the windowAdapter that creates the customized info windows
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            // create and fill the Info Window
            @Override
            public View getInfoContents(Marker marker) {
                View v = getLayoutInflater().inflate(R.layout.info_window, null);
                TextView tvLocality = (TextView) v.findViewById(R.id.tv_locality);
                TextView tvSnippet = (TextView) v.findViewById(R.id.tv_Snippet);
                TextView tvRaius = (TextView) v.findViewById(R.id.tv_radius_description);
                TextView tvLat = (TextView) v.findViewById(R.id.tv_lat);
                TextView tvLong = (TextView) v.findViewById(R.id.tv_long);

                LatLng ll = marker.getPosition();
                tvLocality.setText(marker.getTitle());
                tvSnippet.setText(marker.getSnippet());
                tvRaius.setText("The circle around your location represents a HOWL range of 2.5 miles.");
                tvLat.setText("Latitude: " + ll.latitude);
                tvLong.setText("Longitude: " + ll.longitude);

                return v;
            }
        });
        //marker1.showInfoWindow();
        // add the maker with the following options
        mMarkers.add(marker1); // add the current location marker


        // On Map Click Listener.  It handles setting a marker
        // everywhere else on the map other than myLocation
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //mMarkers.add(mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Map Location")));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                MarkerOptions optionsOnClick = new MarkerOptions()
                        .position(latLng)
                        .title("CLICK WINDOW to SAVE")
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.proftorch))
                        .snippet("If this location is already saved,");
                Marker marker3 = mMap.addMarker(optionsOnClick);
                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                    @Override
                    public View getInfoWindow(Marker marker) {
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {
                        View v = getLayoutInflater().inflate(R.layout.info_window, null);
                        TextView tvLocality = (TextView) v.findViewById(R.id.tv_locality);
                        TextView tvSnippet = (TextView) v.findViewById(R.id.tv_Snippet);
                        TextView tvRadius = (TextView) v.findViewById(R.id.tv_radius_description);
                        TextView tvLat = (TextView) v.findViewById(R.id.tv_lat);
                        TextView tvLong = (TextView) v.findViewById(R.id.tv_long);

                        LatLng ll = marker.getPosition();
                        tvLocality.setText(marker.getTitle());
                        tvSnippet.setText(marker.getSnippet());
                        tvRadius.setText("CLICK to GET HOWLS");
                        tvLat.setText("Latitude: " + ll.latitude);
                        tvLong.setText("Longitude: " + ll.longitude);

                        return v;
                    }
                });
                // add the maker with the following options
                mMarkers.add(marker3);

            }
        });

        // If the user has granted permission, make the current location button appear
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            // If not request for permission
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    //*********** End of On Map Ready area ***********************************

    //*********** Beggining of misc methods area ***********************************

    // This checks to see if the user has given the proper location permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "This app requires location permissions to be granted", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }


    // Remove the markers from the arrayList
    // This method needed to be created so I could
    // keep the yellow location circle on the map
    // when the map.clear() was being called.
    // I added the markers to an arrayList and
    // wipe the list out when the clear button
    // is selected.
    private void removeMarkers() {
        for(Marker marker: mMarkers){
            marker.remove();
        }
        mMarkers.clear();
    }

    // This is a custom getLocation method.  I had
    // to create it so I could find current location
    // upon start up of the app.  This is called and
    // returns a LatLng object.  LatLng contains
    // two Double types as params
    public LatLng getLocation() {
        LatLng latlog = new LatLng(0, 0);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                double latti = location.getLatitude();
                double longi = location.getLongitude();
                LatLng ll = new LatLng(latti, longi);
                latlog = ll;
            }
        }
        return latlog;
    }


    // Draw a circle on the map
    private Circle drawCircle(LatLng latLng){
        CircleOptions options = new CircleOptions()
                .center(latLng)
                .radius(4023) // in meters = 2.5 miles
                .fillColor(0x40ead61c) // 30 is the amount of transparency ead61c is the color yellow
                .strokeColor(Color.BLACK) // this is the outline
                .strokeWidth(3);
        return mMap.addCircle(options);
    }

    /**
     * Asynchronous POST request.
     * All network operations have to be done off the main thread.
     * This allows UI to function normally while this is done in background
     */
    public class SendPostRequest extends AsyncTask<String, Void, String>{
        protected String doInBackground(String... arg0){
            HttpURLConnection myConnection = null;
            try{
                URL owlHowlPostEndpoint = new URL("http://ec2-34-230-76-33.compute-1.amazonaws.com:8080/message");
                //build request data
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("message", arg0[0]);
                params.put("lat", getLocation().latitude);
                params.put("lng", getLocation().longitude);
                params.put("deviceName", identifier);
                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    if (postData.length() != 0) {
                        postData.append('&');
                    }
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                }
                byte[] postDataBytes =postData.toString().getBytes("UTF-8");

                //Set connection
                myConnection = (HttpURLConnection) owlHowlPostEndpoint.openConnection();
                myConnection.setReadTimeout(10000);
                myConnection.setConnectTimeout(10000);
                myConnection.setRequestMethod("POST");
                myConnection.setDoOutput(true);
                myConnection.setDoInput(true);
                myConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                myConnection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                //Write the data
                myConnection.getOutputStream().write(postDataBytes);

                int responseCode = myConnection.getResponseCode();

                if(responseCode == HttpURLConnection.HTTP_OK){
                    return readInput(myConnection.getInputStream());
                }
                else{
                    return "HTTP Error : "+responseCode;
                }
            }catch(Exception e){
                return "Caught exception: "+e.getMessage();
            }finally{
                myConnection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String result){

            Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Gets the data from the database upon request.
     */
    public class SendGetRequest extends AsyncTask<Double, Void, JSONArray>{
        protected JSONArray doInBackground(Double... arg0){
            HttpURLConnection myConnection = null;
            try{
                //build URL/Get data
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("lat", arg0[0]);
                params.put("lng", arg0[1]);
                StringBuilder URLend = new StringBuilder();
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    if (URLend.length() != 0) {
                        URLend.append('&');
                    }
                    URLend.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    URLend.append('=');
                    URLend.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                }

                URL owlHowlGetEndpoint = new URL("http://ec2-34-230-76-33.compute-1.amazonaws.com:8080/message?" + URLend.toString());

                //Set connection
                myConnection = (HttpURLConnection) owlHowlGetEndpoint.openConnection();
                myConnection.setReadTimeout(10000);
                myConnection.setConnectTimeout(10000);

                int responseCode = myConnection.getResponseCode();

                if(responseCode == HttpURLConnection.HTTP_OK){
                    String input = readInput(myConnection.getInputStream());
                    //input = input.substring(1,input.length()-1);
                    JSONArray json = new JSONArray(input);
                    howls = json;
                    return json;
                }
                else{
                    return new JSONArray("[{\"error\":\""+responseCode+"\"}]");
                }
            }catch(Exception e){
                return null;
            }finally{
                myConnection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            //opens view of messages
            Intent myIntent = new Intent(getApplicationContext(), rowan.owlhowl.List.class);
            myIntent.putExtra("howls", howls.toString());
            myIntent.putExtra("identifier", identifier);
            // This allows the exchange of returned data from the database
            // to be handed over tho class List.
            startActivity(myIntent);
        }
    }

    private String readInput(InputStream input) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(input));
        StringBuffer sb = new StringBuffer("");
        String line = "";
        while((line = in.readLine()) != null){
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }

    /**
     * Gets the saved LatLng objects in the savedLocations Arraylist and
     * creates a custom marker.  It then displays them on the map.
     */
    public void getSavedLocations(){
        if(savedLocations.isEmpty()) {
            Toast.makeText(MapsActivityOwlHowl.this,"There are no Saved Locations. Please pick some to add.",Toast.LENGTH_LONG).show();
        }
        if(savedLocations != null){
            for(LatLng s: savedLocations){
                MarkerOptions options5 = new MarkerOptions()
                        .position(s)
                        .title("SAVED MAP LOCATION")
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.heart2))
                        .snippet("CLICK to GET HOWLS");
                Marker savLocMark = mMap.addMarker(options5);
                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                    @Override
                    public View getInfoWindow(Marker marker) {
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {
                        View v = getLayoutInflater().inflate(R.layout.info_window, null);
                        TextView tvLocality = (TextView) v.findViewById(R.id.tv_locality);
                        TextView tvSnippet = (TextView) v.findViewById(R.id.tv_Snippet);
                        TextView tvRaius = (TextView) v.findViewById(R.id.tv_radius_description);
                        TextView tvLat = (TextView) v.findViewById(R.id.tv_lat);
                        TextView tvLong = (TextView) v.findViewById(R.id.tv_long);

                        LatLng ll = marker.getPosition();
                        tvLocality.setText(marker.getTitle());
                        tvSnippet.setText(marker.getSnippet());
                        tvRaius.setText("or LONG CLICK to DELETE");
                        tvLat.setText("Latitude: " + ll.latitude);
                        tvLong.setText("Longitude: " + ll.longitude);

                        return v;
                    }
                });
                // add the maker with the following options
                mMarkers.add(savLocMark);

            }
        }
    }

    //*********** End of misc methods area ***********************************
}