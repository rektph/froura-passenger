package com.froura.develo4.passenger.booking;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.froura.develo4.passenger.LandingActivity;
import com.froura.develo4.passenger.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FindNearbyDriverActivity extends AppCompatActivity {

    private CountDownTimer timer;
    private CountDownTimer searchTimer;
    private TextView cntDwnTxtVw;
    private String uid;
    private DatabaseReference bookingRef;
    private DatabaseReference acceptedRef;
    private DatabaseReference driver;
    private boolean bookingAccepted = false;

    private RelativeLayout loading_view;
    private Button btnCancel;
    private String driver_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_nearby_driver);

        uid = FirebaseAuth.getInstance().getUid();
        cntDwnTxtVw = findViewById(R.id.cntDwnTxtVw);
        btnCancel = findViewById(R.id.btnCancel);
        loading_view = findViewById(R.id.loading_view);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        bookingRef = FirebaseDatabase.getInstance().getReference().child("services").child("booking").child(uid);
        timer = new CountDownTimer(6000, 1000) {
            @Override
            public void onTick(long l) {
                cntDwnTxtVw.setText("CANCEL IN " + l / 1000);
            }

            @Override
            public void onFinish() {
                sendBooking();
            }
        };
        timer.start();
        searchTimer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long l) { }

            @Override
            public void onFinish() {
                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("users/driver/"+driver_id+"/nearest_passenger");
                ref.removeValue();
                bookingRef.removeValue();
                noDriverNearby();
            }
        };
    }

    private void sendBooking() {
        searchTimer.start();
        cntDwnTxtVw.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        loading_view.setVisibility(View.VISIBLE);
        bookingRef.child("pickup").child("name").setValue(getIntent().getStringExtra("pickupName"));
        bookingRef.child("pickup").child("lat").setValue(getIntent().getDoubleExtra("pickupLat", 0));
        bookingRef.child("pickup").child("lng").setValue(getIntent().getDoubleExtra("pickupLng", 0));
        bookingRef.child("dropoff").child("name").setValue(getIntent().getStringExtra("dropoffName"));
        bookingRef.child("dropoff").child("lat").setValue(getIntent().getDoubleExtra("dropoffLat", 0));
        bookingRef.child("dropoff").child("lng").setValue(getIntent().getDoubleExtra("dropoffLng", 0));
        bookingRef.child("fare").setValue(getIntent().getStringExtra("fare"));

        acceptedRef = FirebaseDatabase.getInstance().getReference("services/booking/"+uid+"/accepted_by");
        acceptedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null) {
                    searchTimer.cancel();
                    bookingAccepted = true;
                    Intent intent = new Intent(FindNearbyDriverActivity.this, DriverAcceptedActivity.class);
                    intent.putExtra("driver_id",dataSnapshot.getValue().toString());
                    startActivity(intent);
                    finish();
                    acceptedRef.removeEventListener(this);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void noDriverNearby() {
        Intent intent = new Intent(this, LandingActivity.class);
        intent.putExtra("noDriver", 1);
        intent.putExtra("driverId", driver_id);
        intent.putExtra("hasPickup", 1);
        intent.putExtra("hasDropoff", 1);
        if(getIntent().getStringExtra("pickupPlaceId") != null)
            intent.putExtra("pickupPlaceId", getIntent().getStringExtra("pickupPlaceId"));
        else {
            intent.putExtra("pickupName", getIntent().getStringExtra("pickupName"));
            intent.putExtra("pickupLatLng", getIntent().getDoubleExtra("pickupLat", 0)
                    + "," + getIntent().getDoubleExtra("pickupLng", 0));
            intent.putExtra("pickupLat", getIntent().getDoubleExtra("pickupLat", 0));
            intent.putExtra("pickupLng", getIntent().getDoubleExtra("pickupLng", 0));
        }
        if(getIntent().getStringExtra("dropoffPlaceId") != null)
            intent.putExtra("dropoffPlaceId", getIntent().getStringExtra("dropoffPlaceId"));
        else {
            intent.putExtra("dropoffName", getIntent().getStringExtra("dropoffName"));
            intent.putExtra("dropoffLatLng", getIntent().getDoubleExtra("dropoffLat", 0)
                    + "," + getIntent().getDoubleExtra("dropoffLng", 0));
            intent.putExtra("dropoffLat", getIntent().getDoubleExtra("dropoffLat", 0));
            intent.putExtra("dropoffLng", getIntent().getDoubleExtra("dropoffLng", 0));
        }
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        bookingRef.removeValue();
        Intent intent = new Intent(FindNearbyDriverActivity.this, LandingActivity.class);
        intent.putExtra("hasPickup", 1);
        intent.putExtra("hasDropoff", 1);
        if(getIntent().getStringExtra("pickupPlaceId") != null)
            intent.putExtra("pickupPlaceId", getIntent().getStringExtra("pickupPlaceId"));
        else {
            intent.putExtra("pickupName", getIntent().getStringExtra("pickupName"));
            intent.putExtra("pickupLatLng", getIntent().getDoubleExtra("pickupLat", 0)
                    + "," + getIntent().getDoubleExtra("pickupLng", 0));
            intent.putExtra("pickupLat", getIntent().getDoubleExtra("pickupLat", 0));
            intent.putExtra("pickupLng", getIntent().getDoubleExtra("pickupLng", 0));
        }
        if(getIntent().getStringExtra("dropoffPlaceId") != null)
            intent.putExtra("dropoffPlaceId", getIntent().getStringExtra("dropoffPlaceId"));
        else {
            intent.putExtra("dropoffName", getIntent().getStringExtra("dropoffName"));
            intent.putExtra("dropoffLatLng", getIntent().getDoubleExtra("dropoffLat", 0)
                    + "," + getIntent().getDoubleExtra("dropoffLng", 0));
            intent.putExtra("dropoffLat", getIntent().getDoubleExtra("dropoffLat", 0));
            intent.putExtra("dropoffLng", getIntent().getDoubleExtra("dropoffLng", 0));
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!bookingAccepted) bookingRef.removeValue();
        timer.cancel();
        searchTimer.cancel();
    }
}
