package majd_hamdan.com.easyjob.job;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import majd_hamdan.com.easyjob.R;
import majd_hamdan.com.easyjob.authentication.User;
import majd_hamdan.com.easyjob.ui.HistoryFragment;


public class JobDetailsActivity extends AppCompatActivity {

    private int job_code;//tells which view we are dealing with(past, current or created)

    private Job job;
    private String user_id;

    // to get a job creator's number of that is necessary
    private String creatorNumber;

    String TAG = "mh";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_details);

        // set the app bar color and title
        setTitle("Job Details");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.primaryColor)));

        // used to suppress the keyboard from popping up when code runs
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


        //initiate view ui
        // find job code and job from saved instance state if activity is destroyed
        if(savedInstanceState != null){
            user_id = savedInstanceState.getString(HistoryFragment.USER_ID_TAG);
            job = (Job) savedInstanceState.getSerializable(HistoryFragment.JOB_KEY);
        }else{
            user_id = getIntent().getStringExtra(HistoryFragment.USER_ID_TAG);
            job = (Job) getIntent().getSerializableExtra(HistoryFragment.JOB_KEY);
        }

//        display the correct UI depending on the job properties
        // display the job as created by current user if the current user is the creator
        if(job.creator_id.equals(user_id)){
            findViewById(R.id.created_view).setVisibility(View.VISIBLE);
            findViewById(R.id.past_view).setVisibility(View.GONE);
            findViewById(R.id.current_view).setVisibility(View.GONE);
            findViewById(R.id.avaliable_job_view).setVisibility(View.GONE);
            initiate_created_ui();
        }else{
            // display the available job UI if the job is available for the take
            if(job.isAvailable){
                findViewById(R.id.avaliable_job_view).setVisibility(View.VISIBLE);
                findViewById(R.id.current_view).setVisibility(View.GONE);
                findViewById(R.id.created_view).setVisibility(View.GONE);
                findViewById(R.id.past_view).setVisibility(View.GONE);
                initiate_available_job_ui();
            }else{
                // display past job UI as the job is done
                if(job.isDone_worker){
                    findViewById(R.id.past_view).setVisibility(View.VISIBLE);
                    findViewById(R.id.currentView).setVisibility(View.GONE);
                    findViewById(R.id.created_view).setVisibility(View.GONE);
                    findViewById(R.id.avaliable_job_view).setVisibility(View.GONE);
                    initiate_past_ui();
                }
                // the job must still be going so display current job UI
                else{
                    findViewById(R.id.current_view).setVisibility(View.VISIBLE);
                    findViewById(R.id.past_view).setVisibility(View.GONE);
                    findViewById(R.id.created_view).setVisibility(View.GONE);
                    findViewById(R.id.avaliable_job_view).setVisibility(View.GONE);
                    initiate_current_ui();
                }
            }
        }

    }


    public void initiate_current_ui(){
        TextView type = findViewById(R.id.job_type);
        TextView description = findViewById(R.id.description);
        TextView job_pay = findViewById(R.id.job_pay);
        TextView name = findViewById(R.id.creator_name);
        TextView location = findViewById(R.id.location);
        type.setText("Job Type: " + job.type);
        description.setText("Job Description: " + job.description);
        job_pay.setText("$" + job.hourlyPay);
        fetch_user_info(name, job.creator_id);
        location.setText(job.address);
    }

    public void initiate_past_ui(){

        EditText job_type;
        EditText job_pay;
        EditText job_description;
        EditText address_field;
        EditText city_field;
        EditText zipcode_field;
        EditText state_field;
        EditText country_field;
        EditText creater;
        EditText crt_fb;
        EditText ur_fb;
        job_type = (EditText) findViewById(R.id.jobtypeField_p);
        job_pay = (EditText) findViewById(R.id.payField_p);
        job_description = (EditText) findViewById(R.id.descriptionField_p);
        address_field = (EditText) findViewById(R.id.addressField_p);
        city_field = (EditText) findViewById(R.id.cityField_p);
        zipcode_field = (EditText) findViewById(R.id.zipcodeField_p);
        country_field = (EditText) findViewById(R.id.countryField_p);
        state_field = (EditText) findViewById(R.id.stateField_p);
        creater = (EditText) findViewById(R.id.creater);
        crt_fb = (EditText) findViewById(R.id.creater_fb);
        ur_fb = (EditText) findViewById(R.id.your_fb);

        job_type.setText(job.type);
        job_description.setText(job.description);
        job_pay.setText(job.hourlyPay);

        String[] add = job.address.split(",");
        String[] state_zip = add[2].split(" ");
        address_field.setText(add[0]);
        city_field.setText(add[1]);
        state_field.setText(state_zip[1]);
        country_field.setText(add[3]);
        zipcode_field.setText(state_zip[2]);

        fetch_user_info(creater, job.creator_id);
        crt_fb.setText(job.creator_feedback);
        ur_fb.setText(job.worker_feedback);

    }

    public void initiate_created_ui(){
        // references for the job profile information

        EditText job_type;
        EditText job_pay;
        EditText job_description;
        EditText address_field;
        EditText city_field;
        EditText zipcode_field;
        EditText state_field;
        EditText country_field;
        EditText worker;

        // initialize the edit texts for the job profile information

        job_type = (EditText) findViewById(R.id.jobtypeField);
        job_pay = (EditText) findViewById(R.id.payField);
        job_description = (EditText) findViewById(R.id.descriptionField);
        address_field = (EditText) findViewById(R.id.addressField);
        city_field = (EditText) findViewById(R.id.cityField);
        zipcode_field = (EditText) findViewById(R.id.zipcodeField);
        country_field = (EditText) findViewById(R.id.countryField);
        state_field = (EditText) findViewById(R.id.stateField);
        worker = (EditText) findViewById(R.id.worker);

        // set edit text information from what the user input
        job_type.setText(job.type);
        job_description.setText(job.description);
        job_pay.setText(job.hourlyPay);
        String[] add = job.address.split(",");
        String[] state_zip = add[2].split(" ");
        address_field.setText(add[0]);
        city_field.setText(add[1]);
        state_field.setText(state_zip[1]);
        country_field.setText(add[3]);
        zipcode_field.setText(state_zip[2]);


        if(job.isDone_worker){
            job_description.setEnabled(false);
            job_pay.setEnabled(false);
            job_type.setEnabled(false);
            state_field.setEnabled(false);
            address_field.setEnabled(false);
            city_field.setEnabled(false);
            zipcode_field.setEnabled(false);
            country_field.setEnabled(false);
            worker.setVisibility(View.VISIBLE);
            fetch_user_info(worker, job.worker_id);
        }else{
            if(!job.isAvailable){
                job_description.setEnabled(false);
                job_pay.setEnabled(false);
                job_type.setEnabled(false);
                state_field.setEnabled(false);
                address_field.setEnabled(false);
                city_field.setEnabled(false);
                zipcode_field.setEnabled(false);
                country_field.setEnabled(false);
                worker.setVisibility(View.VISIBLE);
                findViewById(R.id.drop_worker).setVisibility(View.VISIBLE);
                fetch_user_info(worker, job.worker_id);
            }
        }
            }

    public void initiate_available_job_ui(){
        TextView type = findViewById(R.id.job_type_a);
        TextView description = findViewById(R.id.description_a);
        TextView job_pay = findViewById(R.id.job_pay_a);
        TextView name = findViewById(R.id.creator_name_a);
        TextView location = findViewById(R.id.location_a);
        type.setText("Job Type: " + job.type);
        description.setText("Job Desctribtion: " + job.description);
        job_pay.setText("$" + job.hourlyPay);
        fetch_user_info(name, job.creator_id);
        location.setText(job.address);

    }


    protected void onSaveInstanceState(Bundle outState) {
        // create onSaveInstanceState to retrieve information on resume or restart of activity

        super.onSaveInstanceState(outState);
        outState.putString(HistoryFragment.USER_ID_TAG, user_id);
        outState.putSerializable(HistoryFragment.JOB_KEY, job);
    }

    public void fetch_user_info(TextView view, String creator_id ){
        // get user info from data base

        DatabaseReference users_ref = FirebaseDatabase.getInstance().getReference("users");
        Query userQuery = users_ref.child(creator_id);
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                creatorNumber = user.phoneNumber;

                if (user != null) {
                    view.setText(user.firstName + " " + user.lastName);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void onGoBackClicked(View view){
        finish();
    }

    public void onAcceptClicked(View view){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        job.worker_id = userId;
        job.isAvailable = false;

        //update the job as not avaliable in both user offers created list and offers list
        DatabaseReference offers_ref = FirebaseDatabase.getInstance().getReference("offers");
        offers_ref.child(job.location_key).setValue(job);




        //add job to offers accepted by current user
        DatabaseReference users_ref = FirebaseDatabase.getInstance().getReference("users");
        users_ref.child(userId).child("offers_accepted").child(job.location_key).setValue(job);

        users_ref.child(job.creator_id).child("offers_created").child(job.location_key).setValue(job);

        Toast toast = Toast.makeText(this, "You have accepted a Job!", Toast.LENGTH_SHORT);
        toast.show();

        finish();



    }


    //Buttons======================================================================================
    public void onCancelClicked(View view){
        finish();
    }

    public void onChatClickedDetails(View view){
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+creatorNumber));
        startActivity(intent);
    }

    public void onContactCreatorClicked(View view){
        // call phone activity to call the creator of the job
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+creatorNumber));
        startActivity(intent);
    }

    public void onDropClicked(View view){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        job.worker_id = "";
        job.isAvailable = true;

        //update the job as avaliable in both user offers created list and offers list
        DatabaseReference offers_ref = FirebaseDatabase.getInstance().getReference("offers");
        offers_ref.child(job.location_key).setValue(job);




        //add job to offers accepted by current user
        DatabaseReference users_ref = FirebaseDatabase.getInstance().getReference("users");
        users_ref.child(userId).child("offers_accepted").child(job.location_key).removeValue();

        users_ref.child(job.creator_id).child("offers_created").child(job.location_key).setValue(job);

        Toast toast = Toast.makeText(this, "You have dropped a Job!", Toast.LENGTH_SHORT);
        toast.show();

        finish();

    }

    public void onSaveClicked(View view){
        finish();
    }

    public void onDeleteButtonClicked(View view){

        //remove offer from offers c
        DatabaseReference offers_ref = FirebaseDatabase.getInstance().getReference("offers");
        offers_ref.child(job.location_key).removeValue();

        //remove offer from user created
        DatabaseReference users_ref = FirebaseDatabase.getInstance().getReference("users");
        users_ref.child(job.creator_id).child("offers_created").child(job.location_key).removeValue();

        Toast toast = Toast.makeText(this, "You have deleted a Job!", Toast.LENGTH_SHORT);
        toast.show();
        finish();
    }



}
