package com.amhealthbot.Login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amhealthbot.MainActivity;
import com.amhealthbot.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity{

    private ImageView Login_Google;

    private ProgressDialog mProgresDialog;

    private FirebaseAuth mAuth;
    private DatabaseReference mUsersDatabase;
    GoogleSignInClient mGoogleSignInClient;

    private static final int RC_SIGN_IN = 234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextView textView = (TextView) findViewById(R.id.login_terms_condition_txt);

        SpannableStringBuilder spanTxt = new SpannableStringBuilder(
                "Click Sign up with Google above to accept our\n");
        spanTxt.append("Terms of Service");
        spanTxt.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Open_Terms_of_service();
            }
        }, spanTxt.length() - "Terms of Service".length(), spanTxt.length(), 0);
        spanTxt.append(" and  ");
        spanTxt.append("Privacy Policy");
        spanTxt.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Open_Privacy_policy();
            }
        }, spanTxt.length() - "Privacy Policy".length(), spanTxt.length(), 0);

        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(spanTxt, TextView.BufferType.SPANNABLE);

        mAuth = FirebaseAuth.getInstance();

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);

        mProgresDialog = new ProgressDialog(this);

        Login_Google = findViewById(R.id.login_google);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(LoginActivity.this, gso);

        Login_Google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();

            }
        });

    }

    private void Open_Terms_of_service() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("https://amhealthbot.flycricket.io/terms.html"));
        startActivity(i);
    }

    public void Open_Privacy_policy(){
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("https://amhealthbot.flycricket.io/privacy.html"));
        startActivity(i);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            finish();
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //if the requestCode is the Google Sign In code that we defined at starting
        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);

                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {

        mProgresDialog.setTitle("Logging In");
        mProgresDialog.setMessage("Authenticating User..");
        mProgresDialog.show();
        mProgresDialog.setCanceledOnTouchOutside(false);

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){

                            String profie_image, Email;

                            if(acct.getPhotoUrl()!=null) {
                                profie_image = acct.getPhotoUrl().toString();
                            }else {
                                profie_image="";
                            }
                            if (acct.getEmail()!=null){
                                Email = acct.getEmail().toString();
                            }else{
                                Email="";
                            }
                            final HashMap userMap = new HashMap<>();
                            userMap.put("email", Email);
                            userMap.put("name", acct.getGivenName());
                            userMap.put("profile_image",profie_image);
                            userMap.put("user_id", mAuth.getCurrentUser().getUid());

                            FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
                            final String uid = current_user.getUid();

                            mUsersDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (snapshot.hasChild(uid)) {
                                        Intent setupIntent = new Intent(LoginActivity.this, MainActivity.class);
                                        setupIntent.putExtra("access","false");
                                        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(setupIntent);
                                        mProgresDialog.dismiss();
                                        finish();
                                    }else{
                                        mUsersDatabase.child(uid).setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                                if (task.isSuccessful()) {

                                                    Intent setupIntent = new Intent(LoginActivity.this, MainActivity.class);
                                                    setupIntent.putExtra("access","false");
                                                    setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    startActivity(setupIntent);
                                                    mProgresDialog.dismiss();
                                                    finish();

                                                }

                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });


                        }
                    }
                });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

}

