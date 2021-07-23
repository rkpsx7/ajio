package com.example.ajio.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ajio.R;
import com.example.ajio.adapter.ProductAdapter;
import com.example.ajio.databinding.ActivityBagBinding;
import com.example.ajio.interfaces.OnClickListener;
import com.example.ajio.model.ProductModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BagActivity extends AppCompatActivity implements OnClickListener {

    private static final int SIGN_IN_KEY = 1;
    private FirebaseAuth mAuth;
    private ActivityBagBinding mBinding;
    private List<ProductModel> mList;
    private ProductAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bag);

        mBinding = ActivityBagBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        checkLoginState();

        mAuth = FirebaseAuth.getInstance();

        mList = new ArrayList<>();
        mAdapter = new ProductAdapter(mList, this, this);
        mBinding.recyclerView.setAdapter(mAdapter);

        addData();

        mBinding.imgCross.setOnClickListener(v -> finish());
        mBinding.btnContinueShopping.setOnClickListener(v -> finish());
        mBinding.btnLoginJoin.setOnClickListener(v -> signInWithGoogle());
        mBinding.ivFavourite.setOnClickListener(v-> startActivity(new Intent(BagActivity.this, WishlistActivity.class)));
    }

    private void addData() {

        FirebaseDatabase.getInstance().getReference("Orders").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    mBinding.recyclerView.setVisibility(View.VISIBLE);
                    mBinding.rlEmptyBagInfo.setVisibility(View.GONE);
                    mBinding.tvBagIsEmpty.setVisibility(View.GONE);

                    mList.clear();

                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {

                        ProductModel model = snapshot1.getValue(ProductModel.class);

                        mList.add(model);
                    }

                    mAdapter.notifyDataSetChanged();

                } else {

                    mBinding.recyclerView.setVisibility(View.VISIBLE);
                    mBinding.rlEmptyBagInfo.setVisibility(View.GONE);
                    mBinding.tvBagIsEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(BagActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void signInWithGoogle() {

        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient signInClient = GoogleSignIn.getClient(this, gso);

        Intent signInIntent = signInClient.getSignInIntent();
        startActivityForResult(signInIntent, SIGN_IN_KEY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SIGN_IN_KEY) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {

                GoogleSignInAccount account = task.getResult(ApiException.class);

                assert account != null;

                firebaseAuthWithGoogle(account.getIdToken());

            } catch (ApiException e) {

                Toast.makeText(this, e.getMessage() + "", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        Toast.makeText(BagActivity.this, "Login Success", Toast.LENGTH_SHORT).show();

                        saveLoginState();

                    } else {

                        Toast.makeText(BagActivity.this, task.getException() + "", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveLoginState() {

        SharedPreferences.Editor preferences = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        preferences.putBoolean("loggedIn", true);
        preferences.apply();

        checkLoginState();
    }

    private void checkLoginState() {

        SharedPreferences preferences = getSharedPreferences("PREFS", MODE_PRIVATE);
        boolean loggedInAlready = preferences.getBoolean("loggedIn", false);

        if (loggedInAlready) {

            mBinding.btnLoginJoin.setVisibility(View.GONE);
            mBinding.tvEmptyDescription.setVisibility(View.GONE);

        } else {

            mBinding.btnLoginJoin.setVisibility(View.VISIBLE);
            mBinding.tvEmptyDescription.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onProductClick(int position) {

        Toast.makeText(this, "You have already purchased this item", Toast.LENGTH_SHORT).show();
    }

    public void onProductClick(View view) {
        startActivity(new Intent(BagActivity.this,HomeActivity.class));
        finish();
    }
}