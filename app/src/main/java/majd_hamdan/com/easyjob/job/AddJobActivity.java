package majd_hamdan.com.easyjob.job;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import majd_hamdan.com.easyjob.Job;
import majd_hamdan.com.easyjob.R;
import majd_hamdan.com.easyjob.authentication.LoginActivity;

public class AddJobActivity extends AppCompatActivity  {

    private DatabaseReference database;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private String userId;
    private EditText job_type;
    private EditText job_pay;
    private EditText address_field;
    private EditText city_field;
    private EditText zipcode_field;
    private EditText state_field;
    private EditText country_field;
    private FusedLocationProviderClient fusedLocationClient;
    private Location user_location;

    String TAG = "mh";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_job);

        //initialize firebase db and user id
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            // Not logged in, launch the Log In activity
            loadLogIn();
        } else {
            userId = firebaseUser.getUid();
        }
        database = FirebaseDatabase.getInstance().getReference();

        // Add items via the Button and EditText at the bottom of the view.
        job_type = (EditText) findViewById(R.id.jobtypeField);
        job_pay = (EditText) findViewById(R.id.payField);
        address_field = (EditText) findViewById(R.id.addressField);
        city_field = (EditText) findViewById(R.id.cityField);
        zipcode_field = (EditText) findViewById(R.id.zipcodeField);
        country_field = (EditText) findViewById(R.id.countryField);
        state_field = (EditText) findViewById(R.id.stateField);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //add user current address to address ui
        autofill_address();

    }

    public void onAddJobClicked(View view){
        String type = job_type.getText().toString();
        Double pay = Double.valueOf(String.valueOf(job_pay.getText()));
        Job job = new Job("", "", 0, type, pay);
        database.child("users").child(userId).child("offers_created").push().setValue(job);
    }

    @SuppressLint("MissingPermission")
    public void autofill_address(){

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            String[] add;
                            Geocoder geocoder = new Geocoder(AddJobActivity.this, Locale.getDefault());
                            try {
                                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                Address address = addresses.get(0);
                                add = address.getAddressLine(0).split(",");
                                fill_address_fields(add[0], add[1], address.getAdminArea(), add[3], address.getPostalCode());
                            } catch (IOException e) {
                            }
                        }
                    }
                });
    }

    public void fill_address_fields(String address, String city, String state, String country, String zipcode){
        address_field.setText(address);
        city_field.setText(city);
        state_field.setText(state);
        country_field.setText(country);
        zipcode_field.setText(zipcode);
    }

    public void loadLogIn(){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
