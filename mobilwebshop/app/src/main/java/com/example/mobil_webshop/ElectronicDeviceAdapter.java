package com.example.mobil_webshop;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class ElectronicDeviceAdapter extends RecyclerView.Adapter<ElectronicDeviceAdapter.ViewHolder> implements Filterable {
    private static final String LOG_TAG = ElectronicDeviceAdapter.class.getName();
    private ArrayList<ElectronicDevice> mElectronicDeviceData;
    private ArrayList<ElectronicDevice> mElectronicDeviceDataAll;
    private Context mContext;
    private int lastPosition = -1;

    public ElectronicDeviceAdapter(Context context, ArrayList<ElectronicDevice> itemsData) {
        this.mElectronicDeviceData = itemsData;
        this.mElectronicDeviceDataAll = itemsData;
        this.mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ElectronicDeviceAdapter.ViewHolder holder, int position) {
        ElectronicDevice currentItem = mElectronicDeviceData.get(position);

        holder.bindTo(currentItem);

        if(holder.getAdapterPosition()>lastPosition){
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_row);
            holder.itemView.startAnimation(animation);
            lastPosition = holder.getAdapterPosition();
        }
    }

    @Override
    public int getItemCount() {
        return mElectronicDeviceData.size();
    }

    @Override
    public Filter getFilter() {
        return shoppingFilter;
    }

    private Filter shoppingFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<ElectronicDevice> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            if (charSequence == null || charSequence.length() == 0){
                results.count = mElectronicDeviceDataAll.size();
                results.values = mElectronicDeviceDataAll;
            } else{
                String filterPattern = charSequence.toString().toLowerCase().trim();

                for (ElectronicDevice item : mElectronicDeviceDataAll){
                    if (item.getName().toLowerCase().contains(filterPattern)){
                        filteredList.add(item);
                    }
                }

                results.count = filteredList.size();
                results.values = filteredList;
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults filterResults) {
            mElectronicDeviceData = (ArrayList) filterResults.values;
            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mTitleText;
        private TextView mInfoText;
        private TextView mPriceText;
        private ImageView mItemImage;
        private RatingBar mRatingBar;

        public ViewHolder(View itemView) {
            super(itemView);

            this.mTitleText = itemView.findViewById(R.id.itemTitle);
            this.mInfoText = itemView.findViewById(R.id.subTitle);
            this.mPriceText = itemView.findViewById(R.id.price);
            this.mItemImage = itemView.findViewById(R.id.itemImage);
            this.mRatingBar = itemView.findViewById(R.id.ratingBar);
        }

        public void bindTo(ElectronicDevice currentItem) {
            mTitleText.setText(currentItem.getName());
            mInfoText.setText(currentItem.getInfo());
            mPriceText.setText(currentItem.getPrice());
            mRatingBar.setRating(currentItem.getRatedInfo());


            if(FirebaseAuth.getInstance().getCurrentUser().isAnonymous()){
                itemView.findViewById(R.id.delete).setVisibility(View.GONE);
            } else{
                FirebaseFirestore.getInstance().collection("MyUsers").whereEqualTo("email", FirebaseAuth.getInstance().getCurrentUser().getEmail()).get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                    if(document.get("role").toString().equals("Eladó")){
                                        itemView.findViewById(R.id.delete).setVisibility(View.VISIBLE);
                                    }else {
                                        itemView.findViewById(R.id.delete).setVisibility(View.GONE);
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                itemView.findViewById(R.id.delete).setVisibility(View.GONE);
                            }
                        });
            }

            Glide.with(mContext).load(currentItem.getImageResource()).into(mItemImage);
            itemView.findViewById(R.id.add_to_cart).setOnClickListener(view -> {
                Log.d("Activity", "Add cart button clicked!");
                ((ShopListActivity)mContext).updateAlertIcon(currentItem);
            });
            itemView.findViewById(R.id.delete).setOnClickListener(view -> {
                ((ShopListActivity)mContext).deleteItem(currentItem);
            });
        }
    };

};

