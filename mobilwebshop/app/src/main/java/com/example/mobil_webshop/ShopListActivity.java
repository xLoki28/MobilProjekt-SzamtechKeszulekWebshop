package com.example.mobil_webshop;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ShopListActivity extends AppCompatActivity {
    private static final String LOG_TAG = ShopListActivity.class.getName();
    private FirebaseUser user;
    private FirebaseAuth mAuth;

    private RecyclerView mRecyclerView;
    private ArrayList<ElectronicDevice> mItemList;
    private ElectronicDeviceAdapter mAdapter;

    private FrameLayout redCircle;
    private TextView contentTextView;

    private MenuItem pButton;
    private Button dButton;

    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;

    private NotificationHandler mNotificationHandler;
    private JobScheduler mJobScheduler;
    private View inflatedView;

    private int queryLimit = 10;
    private int gridNumber;
    private int cartItems = 0;
    private boolean viewRow = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_list);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            Log.d(LOG_TAG, "Authenticated user!");
        } else {
            Log.d(LOG_TAG, "Unauthenticated user!");
            finish();
        }

        mRecyclerView = findViewById(R.id.recyclerView);

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            gridNumber = 2;
        }else{
            gridNumber = 1;
        }

        mRecyclerView.setLayoutManager(new GridLayoutManager(this, gridNumber));
        mItemList = new ArrayList<>();

        mAdapter = new ElectronicDeviceAdapter(this, mItemList);
        mRecyclerView.setAdapter(mAdapter);

        mFirestore = FirebaseFirestore.getInstance();
        mItems = mFirestore.collection("Items");

        queryData();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        this.registerReceiver(powerReceiver,filter);

        mNotificationHandler = new NotificationHandler(this);
        mJobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);

        setJobScheduler();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action == null){
                return;
            }

            switch (action){
                case Intent.ACTION_POWER_CONNECTED:
                    queryLimit = 30;
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    queryLimit = 20;
                    break;
            }

            queryData();
        }
    };

    private void initializeData() {
        String[] itemList = getResources().getStringArray(R.array.shopping_item_names);
        String[] itemInfo = getResources().getStringArray(R.array.shopping_item_desc);
        String[] itemPrice = getResources().getStringArray(R.array.shopping_item_price);
        TypedArray itemsImageResource = getResources().obtainTypedArray(R.array.shopping_item_images);
        TypedArray itemsRate = getResources().obtainTypedArray(R.array.shopping_item_rates);

        for (int i = 0; i < itemList.length; i++)
            mItems.add(new ElectronicDevice(
                    itemList[i],
                    itemInfo[i],
                    itemPrice[i],
                    itemsRate.getFloat(i,0),
                    itemsImageResource.getResourceId(i,0),
                    0));

        itemsImageResource.recycle();
    }

    private void queryData(){
        mItemList.clear();

        mItems.orderBy("cartedCount", Query.Direction.DESCENDING).limit(queryLimit).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots){
                ElectronicDevice item = document.toObject(ElectronicDevice.class);
                item.setId(document.getId());
                mItemList.add(item);
            }

            if (mItemList.size() == 0){
                initializeData();
                queryData();
            }

            mAdapter.notifyDataSetChanged();
        });
    }

    public void deleteItem(ElectronicDevice item){
        DocumentReference ref = mItems.document(item._getId());

        ref.delete().addOnSuccessListener(success -> {
            Log.d(LOG_TAG, "Item is deleted: " + item._getId());
        }).addOnFailureListener(failure -> {
            Toast.makeText(this, "Item " + item._getId() + " cannot be deleted.", Toast.LENGTH_LONG).show();
        });

        queryData();
        mNotificationHandler.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.shop_list_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(LOG_TAG, s);
                mAdapter.getFilter().filter(s);
                return false;
            }
        });
        pButton = menu.findItem(R.id.setting_button);
        if(user.isAnonymous()){
            pButton.setVisible(false);
        } else{
            pButton.setVisible(true);
        }
        return true;
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
        } else if(item.getItemId()==R.id.cart) {
            Log.d(LOG_TAG, "Cart clicked!");
            return true;
        } else if(item.getItemId()==R.id.view_selector) {
            Log.d(LOG_TAG, "View selector clicked!");
            int orientation = getResources().getConfiguration().orientation;
            if(viewRow){
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    changeSpanCount(item, R.drawable.ic_view_grid, 2);
                }else{
                    changeSpanCount(item, R.drawable.ic_view_grid, 1);
                }
            }else {
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    changeSpanCount(item, R.drawable.ic_view_grid, 3);
                }else{
                    changeSpanCount(item, R.drawable.ic_view_grid, 2);
                }
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem alertMenuItem = menu.findItem(R.id.cart);
        FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView();

        redCircle = (FrameLayout) rootView.findViewById(R.id.view_alert_red_circle);
        contentTextView = (TextView) rootView.findViewById(R.id.view_alert_count_textview);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOptionsItemSelected(alertMenuItem);
            }
        });

        return super.onPrepareOptionsMenu(menu);
    }

    public void updateAlertIcon(ElectronicDevice item) {
        cartItems = (cartItems + 1);
        if(cartItems>0){
            contentTextView.setText(String.valueOf(cartItems));
        }else {
            contentTextView.setText("");
        }
        redCircle.setVisibility((cartItems > 0) ? VISIBLE : GONE);

        mItems.document(item._getId()).update("cartedCount", item.getCartedCount() + 1)
            .addOnFailureListener(failure -> {
                Toast.makeText(this, "Item" + item._getId() + " cannot be changed.", Toast.LENGTH_LONG).show();
            });

        mNotificationHandler.send(item.getName());
        queryData();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(powerReceiver);
    }


    private void setJobScheduler() {
        int networkType = JobInfo.NETWORK_TYPE_UNMETERED;
        int hardDeadLine = 5000;

        ComponentName name = new ComponentName(getPackageName(), NotificationJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(0, name)
                .setRequiredNetworkType(networkType)
                .setRequiresCharging(true)
                .setOverrideDeadline(hardDeadLine);

        mJobScheduler.schedule(builder.build());
        //mJobScheduler.cancel(0);
    }
}