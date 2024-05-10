package com.example.mobil_webshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class ProfileActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    private static final String LOG_TAG = ProfileActivity.class.getName();
    TextView userName;
    TextView userEmail;
    TextView userRole;
    TextView phoneNumberTextView;
    TextView phoneTypeOld;
    TextView addressTextView;
    EditText phoneNumber;
    Spinner phoneType;
    EditText address;
    private MenuItem pButton;
    private FirebaseAuth mAuth;
    private CollectionReference usersRef;

    private FirebaseFirestore mFirestore;
    private CollectionReference mUsers;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mFirestore = FirebaseFirestore.getInstance();
        usersRef = mFirestore.collection("MyUsers");
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();


        Log.d(LOG_TAG,currentUser.getEmail());

        phoneNumber = (EditText) findViewById(R.id.phoneSettingsEditText);

        phoneType = (Spinner) findViewById(R.id.phoneSettingsSpinner);
        phoneType.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.phone_modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        phoneType.setAdapter(adapter);

        address = (EditText) findViewById(R.id.addressSettingsEditText);



        findViewById(R.id.update_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation animation = AnimationUtils.loadAnimation(ProfileActivity.this, R.anim.fade_in);
                findViewById(R.id.update_button).startAnimation(animation);

                String phoneN = phoneNumber.getText().toString();
                String phoneT = phoneType.getSelectedItem().toString();
                String addr = address.getText().toString();
                Log.w(LOG_TAG,currentUser.getEmail());


                usersRef.whereEqualTo("email", currentUser.getEmail()).get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                Log.w(LOG_TAG,phoneN);
                                Log.w(LOG_TAG,phoneT);
                                Log.w(LOG_TAG,addr);
                                Log.w(LOG_TAG, queryDocumentSnapshots.getDocuments().toString());
                                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                    document.getReference().update(
                                            "phoneNumber", phoneN,
                                            "phoneType", phoneT,
                                            "address", addr
                                    );
                                }
                                Intent intent = getIntent();
                                finish();
                                startActivity(intent);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(ProfileActivity.this, "Update failed!", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        Query query = usersRef.whereEqualTo("email", currentUser.getEmail());
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d(LOG_TAG, document.getId() + " => " + document.getData());

                        userName = (TextView) findViewById(R.id.userNameTextView);
                        userName.setText((CharSequence) document.get("username"));

                        userEmail = (TextView) findViewById(R.id.userEmailTextView);
                        userEmail.setText((CharSequence) document.get("email"));

                        userRole = (TextView) findViewById(R.id.roleTextView);
                        userRole.setText((CharSequence) document.get("role"));

                        phoneNumberTextView = (TextView) findViewById(R.id.phoneNumberSettingsTextView);
                        phoneNumberTextView.setText((CharSequence) document.get("phoneNumber"));

                        phoneTypeOld = (TextView) findViewById(R.id.phoneTypeSettingsTextView);
                        phoneTypeOld.setText((CharSequence) document.get("phoneType"));

                        addressTextView = (TextView) findViewById(R.id.addressSettingsTextView);
                        addressTextView.setText((CharSequence) document.get("address"));

                    }
                } else {
                    Log.d(LOG_TAG, "Error getting documents: ", task.getException());
                }
            }


        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        pButton = menu.findItem(R.id.setting_button);
        if(currentUser.isAnonymous()){
            pButton.setVisible(false);
        } else{
            pButton.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.log_out_button) {
            Log.d(LOG_TAG, "Log out clicked!");
            Intent intent = new Intent(this, MainActivity.class);
            FirebaseAuth.getInstance().signOut();
            startActivity(intent);
            finish();
            return true;
        } else if(item.getItemId()==R.id.setting_button) {
            Log.d(LOG_TAG, "Settings clicked!");
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            return true;
        } else if(item.getItemId()==R.id.shop) {
            Log.d(LOG_TAG, "Shop clicked!");
            Intent intent = new Intent(this, ShopListActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selectedItem = parent.getItemAtPosition(position).toString();
        Log.i(LOG_TAG, selectedItem);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

}