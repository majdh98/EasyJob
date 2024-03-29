package majd_hamdan.com.easyjob.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.content.ContextCompat;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import majd_hamdan.com.easyjob.R;

import majd_hamdan.com.easyjob.adapters.GeneralJobCardAdapter;
import majd_hamdan.com.easyjob.authentication.User;
import majd_hamdan.com.easyjob.helper.PermissionUtils;
import majd_hamdan.com.easyjob.job.AddJobActivity;
import majd_hamdan.com.easyjob.job.Job;
import majd_hamdan.com.easyjob.job.JobDetailsActivity;

import static majd_hamdan.com.easyjob.ui.HistoryFragment.JOB_KEY;
import static majd_hamdan.com.easyjob.ui.HistoryFragment.USER_ID_TAG;

public class OffersFragment extends Fragment implements OnMapReadyCallback,
        ClusterManager.OnClusterClickListener<OffersFragment.MapItem>,
        ClusterManager.OnClusterItemClickListener<OffersFragment.MapItem>,
        ClusterManager.OnClusterItemInfoWindowClickListener<OffersFragment.MapItem> {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    //Map and location variables
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private DatabaseReference geofire_db;
    private GeoFire geoFire;
    private Location initial_location;
    private ClusterManager<MapItem> clusterManager;
    private MapItem clickMapItemMarker;
    private String user_id;


    private RecyclerView view;
    private TextView welcomeMessage;
    private Button addJob;
    private List<Job> jobs;

    private RadioButton mapToggle;
    private RadioButton listToggle;

    private int items_queried;
    ArrayList<String> geofire_keys;
    String TAG = "xdmh";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View returnView = inflater.inflate(R.layout.fragment_offers, container, false);

        // welcome message
        welcomeMessage = (TextView)returnView.findViewById(R.id.welcome);

        //get user id
        user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // initialze the geofire keys array list
        geofire_keys = new ArrayList<>();

        // fetch user information to update welcome message
        fetch_user_info_for_welcome(welcomeMessage);


        // toggle switch
        mapToggle = (RadioButton)returnView.findViewById(R.id.Maps);
        listToggle = (RadioButton)returnView.findViewById(R.id.offer);


        mapToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listToggle.setSelected(false);
                mapToggle.setSelected(true);
                returnView.findViewById(R.id.list_view).setVisibility(View.GONE);
                returnView.findViewById(R.id.map_view).setVisibility(View.VISIBLE);
            }
        });

        listToggle.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                listToggle.setSelected(true);
                mapToggle.setSelected(false);
                returnView.findViewById(R.id.list_view).setVisibility(View.VISIBLE);
                returnView.findViewById(R.id.map_view).setVisibility(View.GONE);
            }
        });

        welcomeMessage = (TextView)returnView.findViewById(R.id.welcome);
        addJob = (Button) returnView.findViewById(R.id.createJob);
        addJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), AddJobActivity.class));
            }
        });

        // get recycler view
        view = returnView.findViewById(R.id.recycler);
        view.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        view.setLayoutManager(llm);
        initJobs();

        return returnView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        //sync the google map fragment
        mapFragment.getMapAsync(this);


        geofire_db = FirebaseDatabase.getInstance().getReference().child("geofire");
        geoFire = new GeoFire(geofire_db);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        createLocationRequest();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    initial_location = location;
                    // Update UI with location data
                    // ...
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        map = googleMap;
        //check for permission and enable location layer.
        enableMyLocation();

        // setup clusters
        clusterManager = new ClusterManager<>(getContext(), map);
        clusterManager.setOnClusterClickListener(this);
        clusterManager.setOnClusterItemClickListener(this);
        clusterManager.setOnClusterItemInfoWindowClickListener(this);
        clusterManager.getMarkerCollection().setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                // todo: implement
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(getContext().LAYOUT_INFLATER_SERVICE);

                final View view = inflater.inflate(R.layout.custom_marker, null);

                TextView title = (TextView)view.findViewById(R.id.popUpTitle);
                TextView pay = (TextView)view.findViewById(R.id.popUpPay);
                title.setText(clickMapItemMarker.title);
                pay.setText(clickMapItemMarker.snippet);

                return view;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });
        MapItemMarkerRender renderer = new MapItemMarkerRender(getContext(), map, clusterManager);
        // set clusters listeners
        map.setOnCameraIdleListener(clusterManager);
        map.setOnMarkerClickListener(clusterManager);
        clusterManager.setRenderer(renderer);

    }

    //ListView--------------------------------------------------------------------------------------
    private void initJobs(){
        jobs = new ArrayList<>();
    }

    private void initializeAdapter(){
        GeneralJobCardAdapter adapter = new GeneralJobCardAdapter(jobs);
        view.setAdapter(adapter);
        adapter.setOnItemClickListener(new GeneralJobCardAdapter.OnItemClickListener()
        {
            @Override
            public void onMoreDetailsClick(int position) {
                Intent intent = new Intent(getActivity(), JobDetailsActivity.class);
                intent.putExtra(USER_ID_TAG, user_id);
                intent.putExtra(JOB_KEY, jobs.get(position));
                startActivity(intent);
            }
        });
    }

    //Database--------------------------------------------------------------------------------------
    public static void fetch_user_info_for_welcome(TextView toSetWelcome){
        String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference users_ref = FirebaseDatabase.getInstance().getReference("users");
        Query userQuery = users_ref.child(user_id);
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    // get ui element for welcome message and populate with user info
                    toSetWelcome.setText("Hello, " + user.firstName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // fetch offers around default radius of user from firebase using geofire library
    // geofire will run onKeyEntered every time it finds a key that fall withing the radius.
    // once it finds all the keys it will run onGeoQueryReady where another query to firebase will
    // be run to get all the offers associated with the geofire keys.
    public void fetch_offers(){
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(initial_location.getLatitude(), initial_location.getLongitude()), 3);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                geofire_keys.add(key);
            }


            @Override
            public void onKeyExited(String key) {
                jobs = new ArrayList<>();
                initializeAdapter();
                fetch_offers();
                Log.d(TAG, String.format("Key %s is no longer in the search area", key));
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                jobs = new ArrayList<>();
                initializeAdapter();
                fetch_offers();
                Log.d(TAG, String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onGeoQueryReady() {
                //once the data has been queried, check if the loaded data count equals the
                //queried data. If not, wait until all data is loaded then initializeAdapter
                items_queried = 0;
                if(getContext() != null){
                    // get offers reference
                    DatabaseReference offers_ref = FirebaseDatabase.getInstance().getReference("offers");

                    for(int i = 0; i < geofire_keys.size(); i++){
                        // query each key in geofire_keys
                        Query offersQuery = offers_ref.child(geofire_keys.get(i));
                        offersQuery.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // get the job from db and add it to the jobs array list
                                Job job = dataSnapshot.getValue(Job.class);
                                if(job != null){
                                    jobs.add(job);
                                }
                                items_queried++;
                                Log.d(TAG, "onDataChange: iq " + items_queried );

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }

                // check if all jobs where queried, if not wait for a second before checking again
                check_offers_query_completion();

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                jobs = new ArrayList<>();
                initializeAdapter();
                fetch_offers();
                Log.d(TAG, "There was an error with this query: " + error);
            }
        });

    }


    // check if all jobs where queried, if not wait for a second before checking again.
    // if all jobs are queried, populate map
    public void check_offers_query_completion(){
        Log.d(TAG, "check_offers_query_completion: ");
        Log.d(TAG, "check_offers_query_compl" + geofire_keys.size() + " " + items_queried);
        if(items_queried == geofire_keys.size() ){
            Log.d(TAG, "check_offers_query_completion: adas");
            geofire_keys.clear();
            //populate list
            initializeAdapter();

            //populate map
            populate_map();
        }else{
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    check_offers_query_completion();
                }
            }, 1000);
        }
    }

    // cluster methods 
    @Override
    public boolean onClusterClick(Cluster<MapItem> cluster) {
        // build the lat and lng point in the cluster

        LatLngBounds.Builder builder = LatLngBounds.builder();
        Collection<MapItem> venueMarkers = cluster.getItems();

        for (ClusterItem item : venueMarkers) {
            LatLng venuePosition = item.getPosition();
            builder.include(venuePosition);
        }

        final LatLngBounds bounds = builder.build();

        try { map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception error) {

        }

        return true;
    }

    @Override
    public boolean onClusterItemClick(MapItem item) {
        clickMapItemMarker = item;
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(MapItem item) {
        // start intent with the attached job
        Intent intent = new Intent(getActivity(), JobDetailsActivity.class);
        intent.putExtra(USER_ID_TAG, user_id);
        intent.putExtra(JOB_KEY, item.attached);
        startActivity(intent);
    }

    // customize clusterer
    public class MapItemMarkerRender extends DefaultClusterRenderer<MapItem> {

        private final Context mContext;

        public MapItemMarkerRender(Context context, GoogleMap map, ClusterManager<MapItem> clusterManager) {
            super(context, map, clusterManager);
            mContext = context;
        }

        @Override
        protected void onBeforeClusterItemRendered(MapItem item, MarkerOptions markerOptions) {

        }
        @Override
        protected void onBeforeClusterRendered(Cluster<MapItem> cluster, MarkerOptions markerOptions) {

        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster<MapItem> cluster){
            // cluster if there is more than one marker at one postion
            return cluster.getSize() > 1;
        }

    }

    public void populate_map(){
        Log.d(TAG, "populate_map: ");
        for(int i = 0; i<jobs.size(); i++){
            //            LatLng job_location = getLocationFromAddress(jobs.get(i).address);
            //            Marker marker = map.addMarker(
            //                    new MarkerOptions()
            //                            .position(job_location)
            //                            .title(jobs.get(i).type)
            //                            .snippet("$"+jobs.get(i).hourlyPay));
            //            marker.showInfoWindow();

            LatLng job_location = getLocationFromAddress(jobs.get(i).address);
            double lat = job_location.latitude;
            double lng = job_location.longitude;
            MapItem newItem = new MapItem(jobs.get(i), lat, lng, jobs.get(i).type, "$"+jobs.get(i).hourlyPay);
            clusterManager.addItem(newItem);


            // todo: cluster items?
        }

        
    }

    // to help with map clustering for markers
    public class MapItem implements ClusterItem {
        private double latitude;
        private double longitude;

        private final LatLng position;
        private final String title;
        private final String snippet;
        public Job attached;

        public MapItem(Job thisJob, double lat, double lng, String title, String snippet) {
            latitude = lat;
            longitude = lng;
            position = new LatLng(lat, lng);
            this.title = title;
            this.snippet = snippet;
            attached = thisJob;
        }

        @Override
        public LatLng getPosition() {
            return position;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getSnippet() {
            return snippet;
        }


    }

    public LatLng getLocationFromAddress(String address){
        try {
            Geocoder selected_place_geocoder = new Geocoder(getActivity());
            List<Address> addresses;

            addresses = selected_place_geocoder.getFromLocationName(address, 1);

            if (addresses == null) {
                return null;
            } else {
                Address location = addresses.get(0);
                LatLng job_location = new LatLng(location.getLatitude(), location.getLongitude());
                return job_location;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    //location functions----------------------------------------------------------------------------

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        //update location every 10 sec
        locationRequest.setInterval(10000);
        //get as accurate as possible location (enable the use of gps)
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }


    //Permession------------------------------------------------------------------------------------

    //Check if location permissions are granted.Enables the My Location layer if the fine location
    //permission has been granted. Else, ask for permission.
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (map != null) {
                map.setMyLocationEnabled(true);

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(getActivity(), location -> {

                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                initial_location = location;
                                LatLng latLng = new LatLng(location.getLatitude(),
                                        location.getLongitude());
                                // Logic to handle location object
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                                fetch_offers();
                            }else{
                                Toast toast = Toast.makeText(getActivity(), "Unable to find user location, please restart the app",
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        });
            }
        } else {

//            // Display a dialog with rationale and ask for permission
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_rationale_location)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // After click on Ok, request the permission.
                            // Permission to access the location is missing. Show rationale and request permission
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    //handle the result of requestPermission() called in enablemyLocation()
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d("mh", "onRequestPermissionsResult: ");
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // Enable the my location layer if the permission has been granted.
                enableMyLocation();

            } else {
                // Permission was denied. Display an error message
                // Display the missing permission error dialog when the fragments resume.
                PermissionUtils.PermissionDeniedDialog
                        .newInstance(true).show(getChildFragmentManager(), "dialog");
            }
        }
    }

}
