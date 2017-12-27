package com.froura.develo4.passenger;

import android.content.ContentValues;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.froura.develo4.passenger.libraries.RequestPostString;
import com.froura.develo4.passenger.tasks.CheckUserTasks;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

public class PhoneAuthentication extends AppCompatActivity implements CheckUserTasks.OnLoginDriverTasksListener {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private TextView requestCode;
    private Button verify;
    private EditText verifCode;

    private String mobNum;
    private String email;
    private String name;
    private boolean phoneReg;
    private int register = 0;
    private int login = 0;
    private int phpId = -1;
    private CountDownTimer requestCodeTimer;

    private String TAG = "PhoneAuth";
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_authentication);

        requestCode = findViewById(R.id.txtVw_request_code);
        verify = findViewById(R.id.btn_verify);
        verifCode = findViewById(R.id.et_verif_code);

        mobNum = getIntent().getStringExtra("mobNum");
        email = getIntent().getStringExtra("email");
        name = getIntent().getStringExtra("name");
        phoneReg = getIntent().getBooleanExtra("phoneReg", false);
        
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(mAuth.getCurrentUser() != null) {
                    Intent intent = new Intent(PhoneAuthentication.this, LandingActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };
        
        if(phoneReg) {
            requestCode();
            phoneReg = false;
        }

        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mCode = verifCode.getText().toString();
                if(!mCode.isEmpty()) {
                    signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(mVerificationId, mCode));
                    verifCode.setError(null);
                    verifCode.setCompoundDrawablesWithIntrinsicBounds(0,0, 0,0);
                } else {
                    verifCode.setError("Code is required.");
                    verifCode.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_warning_red_24dp,0);
                }

            }
        });

        requestCodeTimer = new CountDownTimer(60000, 1000) {
                    @Override
                    public void onTick(long l) {
                        requestCode.setText("Request a new code in 00:"+ l/1000);
                        requestCode.setTextColor(getResources().getColor(R.color.textViewColor));
                    }

                    @Override
                    public void onFinish() {
                        requestCode.setText(Html.fromHtml("<u>Request a new code.</u>"));
                        requestCode.setTextColor(getResources().getColor(R.color.textLinkColor));

                        requestCode.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                requestCode();
                            }
                        });
                    }
                };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser();
                            userExist(user.getUid());
                        } else {
                            Log.w("PhoneAuth", "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(PhoneAuthentication.this, "There is an error logging in.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void userExist(String user_id) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("users").child("passenger").child(user_id);
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    registerUser();
                    register = 1;
                } else {
                    login = 1;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void registerUser() {
        String user_id = mAuth.getCurrentUser().getUid();
        DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("users").child("passenger").child(user_id).child("name");
        current_user_db.setValue(name);
        current_user_db = FirebaseDatabase.getInstance().getReference().child("users").child("passenger").child(user_id).child("email");
        current_user_db.setValue(email);
        current_user_db = FirebaseDatabase.getInstance().getReference().child("users").child("passenger").child(user_id).child("mobnum");
        current_user_db.setValue(mobNum);
        registerToPhp();
    }

    private void registerToPhp() {
        new CheckUserTasks(PhoneAuthentication.this).execute();
    }
    
    private void requestCode() {
        requestCode.setOnClickListener(null);
        requestCodeTimer.start();
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mobNum,
                60,
                TimeUnit.SECONDS,
                PhoneAuthentication.this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Log.w(TAG, "onVerificationFailed", e);
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            //Phone number is incorrect
                            Toast.makeText(PhoneAuthentication.this, "Phone number is incorrect!", Toast.LENGTH_SHORT).show();
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            //Quota has been reached
                            Toast.makeText(PhoneAuthentication.this, "Server Overload! A request has been sent.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        mVerificationId = s;
                    }

                    @Override
                    public void onCodeAutoRetrievalTimeOut(String s) {
                        super.onCodeAutoRetrievalTimeOut(s);
                        Toast.makeText(PhoneAuthentication.this, "Something: " + s, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void parseCheckUserJSONString(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            int login = jsonObject.getInt("login");
            int register = jsonObject.getInt("register");
            phpId = jsonObject.getInt("u_id");

            if(login == 1) {
                Toast.makeText(this, "Sucess logging in.", Toast.LENGTH_SHORT).show();
            } else if(register == 1) {
                Toast.makeText(this, "Sucess registering.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception ignored) {}
    }

    @Override
    public String createCheckUserPostString(ContentValues contentValues) throws UnsupportedEncodingException {
        contentValues.put("register", register);
        contentValues.put("login", login);
        contentValues.put("name", name);
        contentValues.put("email", email);
        contentValues.put("mobile", mobNum);
        contentValues.put("firebase_id", mAuth.getCurrentUser().getUid());

        return RequestPostString.create(contentValues);
    }
}
