package majd_hamdan.com.easyjob.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import majd_hamdan.com.easyjob.R;
import majd_hamdan.com.easyjob.authentication.LoginActivity;
import majd_hamdan.com.easyjob.authentication.User;
import majd_hamdan.com.easyjob.job.Job;

public class UserInfoActivity extends AppCompatActivity {

    protected EditText emailEditText;
    protected EditText passwordEditText;
    protected Button singupButton;
    protected EditText confirmPassword;
    protected EditText firstName;
    protected EditText lastName;
    protected EditText phoneNumber;
    protected ImageView  imageView;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference database;
    private FirebaseUser user;
    private String userId;
    private User userInfo;



    //String TAG = "mh";




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_info);

        //initiate connection to db
        database = FirebaseDatabase.getInstance().getReference();

        //initiate authentication view
        emailEditText = (EditText) findViewById(R.id.authEmailField);
        passwordEditText = (EditText) findViewById(R.id.authPasswordField);

        // Initialize FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        // find image view to reorganize when orientation changes
        imageView = findViewById(R.id.easyjob_imageview);

        // initalize user info to use
        userInfo = new User();

    }

    public void onSaveButtonClicked(View view) {
        // on save button clicked, check if the user has changed any of the profile information,
        // if change happened, then update user info, if no change, use previous information
        String fn = firstName.getText().toString();
        if(fn.isEmpty()){
            fn = userInfo.firstName;
        }
        String ln = lastName.getText().toString();
        if(ln.isEmpty()){
            ln = userInfo.lastName;
        }
        String phone_num = phoneNumber.getText().toString();
        if(phone_num.isEmpty()){
            phone_num = userInfo.phoneNumber;
        }
        String email = emailEditText.getText().toString();
        if(!email.isEmpty()){
            user.updateEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                            }
                        }
                    });
        }else{
            email = userInfo.email;
        }
        String password = passwordEditText.getText().toString();
        //check password and email then create use
        if (!password.isEmpty()) {
            user.updatePassword(password)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {

                            }
                        }
                    });
        }

        User user_info = new User(userId, fn, ln, phone_num, email);
        database.child("users").child(userId).setValue(user_info);
        finish();
    }

    public void set_user_info_to_db_info(){

        //get user id
        userId = user.getUid();

        // find user info from data base
        DatabaseReference users_ref = FirebaseDatabase.getInstance().getReference("users");
        Query userQuery = users_ref.child(userId);
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // if data is changed then reset data base information
                User user  = dataSnapshot.getValue(User.class);
                if(user != null){
                    userInfo = user;
                    firstName.setText(user.firstName);
                    lastName.setText(user.lastName);
                    emailEditText.setText(user.email);
                    phoneNumber.setText(user.phoneNumber);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void onAuthenticateClicked(View view) {

        //get email and password and trim them
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        email = email.trim();
        password = password.trim();

        //check if the correct email and password are given then authenticate
        if (email.isEmpty() || password.isEmpty()) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.login_error_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } else {
            firebaseAuth.signOut();
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        //move to contentActivity if authentication is succelful otherwise diplay
                        //authentication error message.
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                findViewById(R.id.authenticateView).setVisibility(View.GONE);
                                findViewById(R.id.resetView).setVisibility(View.VISIBLE);
                                //initialize views
                                emailEditText = (EditText) findViewById(R.id.emailField);
                                passwordEditText = (EditText) findViewById(R.id.passwordField);
                                singupButton = (Button) findViewById(R.id.singupButton);
                                confirmPassword = (EditText) findViewById(R.id.passwordConfirmationField);
                                firstName = (EditText) findViewById(R.id.firstNameField);
                                lastName = (EditText) findViewById(R.id.lastNameField);
                                phoneNumber = (EditText) findViewById(R.id.phoneNumberField);
                                set_user_info_to_db_info();

                            } else {
                                AlertDialog dialog = new AlertDialog.Builder(UserInfoActivity.this)
                                        .setMessage(task.getException().getMessage())
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show();
                            }
                        }
                    });
        }


    }

    public void onDeleteButtonClicked(View view) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.delete_account)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        DatabaseReference offers_ref = FirebaseDatabase.getInstance().getReference("offers");
                        Query userQuery = offers_ref.orderByChild("creator_id").equalTo(userId);
                        userQuery.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Job job = dataSnapshot.getValue(Job.class);
                                if(!job.isAvailable){
                                    offers_ref.child(job.location_key).removeValue();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        //remove offer from user created
                        DatabaseReference users_ref = FirebaseDatabase.getInstance().getReference("users");
                        users_ref.child(userId).removeValue();

                        user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(UserInfoActivity.this, "User info and profile are deleted!",
                                        Toast.LENGTH_SHORT).show();

                            }


                        });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

    }


    public void loadLogIn(){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
