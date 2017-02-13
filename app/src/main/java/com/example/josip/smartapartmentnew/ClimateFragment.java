package com.example.josip.smartapartmentnew;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
//import android.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Timer;
import java.util.TimerTask;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ClimateFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ClimateFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ClimateFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";

    // TODO: Rename and change types of parameters
    private String mClimateID;

    private DatabaseReference mDatabase;

    private ImageView mImageViewAva;
    private TextView mTextViewTemp;
    private TextView mTextViewHum;

    Timer mTimer;

    private OnFragmentInteractionListener mListener;

    public ClimateFragment() {
        // Required empty public constructor
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment ClimateFragment.
     */
    // TODO: Rename and change types and number of parameters
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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_climate, container, false);

        mImageViewAva = (ImageView)view.findViewById(R.id.imageViewAva1);
        mTextViewTemp = (TextView)view.findViewById(R.id.textViewTemp);
        mTextViewHum = (TextView)view.findViewById(R.id.textViewHum);

        ((Button)view.findViewById(R.id.buttonON)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Button)v).setBackgroundColor(Color.RED);
                mDatabase.child(mClimateID).child("cmdClimate").setValue(1);
            }
        });
        ((Button)view.findViewById(R.id.buttonOFF)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Button)v).setBackgroundColor(Color.RED);
                mDatabase.child(mClimateID).child("cmdClimate").setValue(2);
            }
        });

        mDatabase.child(mClimateID).child("availability").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mImageViewAva.setImageResource(R.drawable.sensor_connected);
                if (mTimer != null)
                {
                    mTimer.purge();
                    mTimer.cancel();
                }
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mImageViewAva.setImageResource(R.drawable.sensor_disconnected);
                            }
                        });

                    }
                }, 5000L);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        mDatabase.child(mClimateID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (Integer.parseInt(dataSnapshot.child("cmdClimate").getValue().toString()) == 0)
                {
                    ((Button)getView().findViewById(R.id.buttonON)).setBackgroundColor(Color.GRAY);
                    ((Button)getView().findViewById(R.id.buttonOFF)).setBackgroundColor(Color.GRAY);
                }
                mTextViewTemp.setText(dataSnapshot.child("temperature").getValue().toString());
                mTextViewHum.setText(dataSnapshot.child("huminidity").getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
