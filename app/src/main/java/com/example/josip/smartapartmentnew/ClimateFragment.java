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
                mTextViewTemp.setText(dataSnapshot.child("temperature").getValue().toString());
                mTextViewHum.setText(dataSnapshot.child("humidity").getValue().toString());
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

        mDatabase.child(mClimateID).child("availability").addValueEventListener(mAvailableListener);
        mDatabase.child(mClimateID).child("Cmd").child("cmdTurn").addValueEventListener(mCmdEventListener);
        mDatabase.child(mClimateID).child("Data").addValueEventListener(mDataEventListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDatabase.child(mClimateID).child("availability").removeEventListener(mAvailableListener);
        mDatabase.child(mClimateID).child("Cmd").removeEventListener(mCmdEventListener);
        mDatabase.child(mClimateID).child("Data").removeEventListener(mDataEventListener);
    }

}
