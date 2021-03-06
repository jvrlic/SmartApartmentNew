package com.example.josip.smartapartmentnew;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;



public class ClimateFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";

    private String mClimateID;

    private DatabaseReference mDatabase;

    private DatabaseReference mAvaRef;
    private DatabaseReference mCmdRef;
    private DatabaseReference mDataRef;

    private ImageView mImageViewAva;
    private TextView mTextViewTemp;
    private TextView mTextViewHum;

    private ImageButton mImageButtonTurnOn;
    private ImageButton mImageButtonTurnOff;

    private ValueEventListener mCmdEventListener;
    private ValueEventListener mDataEventListener;
    private ValueEventListener mAvailableListener;

    private int mIdSensorAvailable;


    public ClimateFragment() {
        // Required empty public constructor
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mIdSensorAvailable = 0;
    }

    public static ClimateFragment newInstance(String param1) {
        ClimateFragment fragment = new ClimateFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mClimateID = getArguments().getString(ARG_PARAM1);
        }

        mCmdEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if ((long)dataSnapshot.getValue() == 0)
                {
                    mImageButtonTurnOn.setBackgroundResource(R.drawable.climate_turngreen);
                    mImageButtonTurnOff.setBackgroundResource(R.drawable.climate_turnred);
                }
                else if ((long)dataSnapshot.getValue() == 1)
                {
                    mImageButtonTurnOn.setBackgroundResource(R.drawable.climate_turngrey);
                }
                else if ((long)dataSnapshot.getValue() == 2)
                {
                    mImageButtonTurnOff.setBackgroundResource(R.drawable.climate_turngrey);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mDataEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int hum_offset = dataSnapshot.child("humidity_offset").getValue(Integer.class);
                int temp_offset = dataSnapshot.child("temperature_offset").getValue(Integer.class);
                mTextViewTemp.setText(String.valueOf(dataSnapshot.child("temperature").getValue(Integer.class) - temp_offset));
                mTextViewHum.setText(String.valueOf(dataSnapshot.child("humidity").getValue(Integer.class) - hum_offset));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mAvailableListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mIdSensorAvailable = mIdSensorAvailable % 3 + 1;

                switch(mIdSensorAvailable) {
                    case 1:
                        mImageViewAva.setImageResource(R.drawable.sensor_available_1);
                        break;
                    case 2:
                        mImageViewAva.setImageResource(R.drawable.sensor_available_2);
                        break;
                    case 3:
                        mImageViewAva.setImageResource(R.drawable.sensor_available_3);
                        break;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_climate, container, false);

        mImageViewAva = (ImageView)view.findViewById(R.id.imageViewAva1);
        mTextViewTemp = (TextView)view.findViewById(R.id.textViewTemp);
        mTextViewHum = (TextView)view.findViewById(R.id.textViewHum);
        mImageButtonTurnOn = (ImageButton) view.findViewById(R.id.imageButtonOn);
        mImageButtonTurnOff = (ImageButton) view.findViewById(R.id.imageButtonOff);

        mImageButtonTurnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatabase.child(mClimateID).child("Cmd").child("cmdTurn").setValue(1);
            }
        });
        mImageButtonTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatabase.child(mClimateID).child("Cmd").child("cmdTurn").setValue(2);
            }
        });

        return view;
    }
    @Override
    public void onStart() {
        super.onStart();

        if (mAvaRef != null)
            mAvaRef.removeEventListener(mAvailableListener);
        mAvaRef =  mDatabase.child(mClimateID).child("availability");
        mAvaRef.addValueEventListener(mAvailableListener);

        if (mCmdRef != null)
            mCmdRef.removeEventListener(mCmdEventListener);
        mCmdRef =  mDatabase.child(mClimateID).child("Cmd").child("cmdTurn");
        mCmdRef.addValueEventListener(mCmdEventListener);

        if (mDataRef != null)
            mDataRef.removeEventListener(mDataEventListener);
        mDataRef =  mDatabase.child(mClimateID).child("Data");
        mDataRef.addValueEventListener(mDataEventListener);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mAvaRef != null)
            mAvaRef.removeEventListener(mAvailableListener);
        if (mCmdRef != null)
            mCmdRef.removeEventListener(mCmdEventListener);
        if (mDataRef != null)
            mDataRef.removeEventListener(mDataEventListener);
    }

}
