package com.example.ajio.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ajio.R;
import com.example.ajio.adapter.ProductAdapter;
import com.example.ajio.databinding.ActivityProductBinding;
import com.example.ajio.interfaces.OnClickListener;
import com.example.ajio.model.ProductModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductActivity extends AppCompatActivity implements PaymentResultListener, OnClickListener {

    private ActivityProductBinding mBinding;
    private ProductAdapter mAdapter;
    private List<ProductModel> mList;
    private int position;
    private DatabaseReference mDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityProductBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        Checkout.preload(getApplicationContext());

        initializeData();
        addData();
    }

    private void initializeData() {

        mList = new ArrayList<>();
        mAdapter = new ProductAdapter(mList, this, this);
        mBinding.recyclerView.setAdapter(mAdapter);
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("ProductDetails");

        mBinding.imgBag.setOnClickListener(v -> startActivity(new Intent(ProductActivity.this, BagActivity.class)));
        mBinding.imgFavourite.setOnClickListener(v -> {
            startActivity(new Intent(ProductActivity.this, WishlistActivity.class));
            finish();
        });
        mBinding.imgSearch.setOnClickListener(v -> startActivity(new Intent(ProductActivity.this, HomeActivity.class)));
        mBinding.imgHamburger.setOnClickListener(v -> startActivity(new Intent(ProductActivity.this, NavigationActivity.class)));
    }

    private void addData() {

        // Retrieving product details from firebase

        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    mList.clear();

                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {

                        ProductModel model = snapshot1.getValue(ProductModel.class);

                        mList.add(model);
                    }

                    adjustView();
                    mAdapter.notifyDataSetChanged();

                } else {

                    adjustView();

                    Toast.makeText(ProductActivity.this, "Error!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                adjustView();

                Toast.makeText(ProductActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onProductClick(int position) {
        this.position = position;

        // Saving login state in shared preference

        SharedPreferences preferences = getSharedPreferences("PREFS", MODE_PRIVATE);
        boolean loggedInAlready = preferences.getBoolean("loggedIn", false);

        if (loggedInAlready) {
            startPayment();

        } else {

            Toast.makeText(this, "Sign in first to purchase this product", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProductActivity.this, AccountActivity.class));
            finish();
        }
    }

    public void startPayment() {

        /*
         * Instantiate Checkout
         */

        Checkout checkout = new Checkout();

        checkout.setKeyID("rzp_test_KGbp2Y9RY4nziY");

        /*
         * Setting logo
         */
        checkout.setImage(R.drawable.ajio);

        /*
         * Reference to current activity
         */
        final Activity activity = this;

        /*
         * Passing payment options to the Razorpay Checkout as a JSONObject
         */
        try {
            JSONObject options = new JSONObject();

            options.put("name", "AJIO");
            options.put("description", "Reference No. #123456");
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png");
            options.put("theme.color", "#3399cc");
            options.put("currency", "INR");
            options.put("amount", "50000");
            options.put("prefill.email", "vipindev@gmail.com");
            options.put("prefill.contact", "8890790340");
            JSONObject retryObj = new JSONObject();
            retryObj.put("enabled", true);
            retryObj.put("max_count", 6);
            options.put("retry", retryObj);

            checkout.open(activity, options);

        } catch (Exception e) {

            Toast.makeText(activity, "Payment failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPaymentSuccess(String s) {

        Toast.makeText(this, "Payment successful", Toast.LENGTH_SHORT).show();

        Map<String, Object> map = new HashMap<>();
        map.put("ordered", true);
        map.put("wishlisted", false);

        mDatabaseReference.child(mList.get(position).getKey()).updateChildren(map).addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                startActivity(new Intent(ProductActivity.this, BagActivity.class));
                finish();
            }
        });
    }

    @Override
    public void onPaymentError(int i, String s) {

        Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show();
    }

    private void adjustView() {

        mBinding.shimmerLayout.setVisibility(View.GONE);
        mBinding.tvPrice.setVisibility(View.VISIBLE);
        mBinding.tvProducts.setVisibility(View.VISIBLE);
        mBinding.recyclerView.setVisibility(View.VISIBLE);
        mBinding.bottomLayout.setVisibility(View.VISIBLE);
    }

}