package com.example.josip.smartapartmentnew;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DoorFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DoorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DoorFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";

    private String mDoorID;

    private ImageView mImageViewAva;
    private ImageButton mImageButtonUnlock;

    private MediaPlayer mMp;
    private Map<Long, String> mKeyNames;

    private int mIdSensorAvailable;

    private MyListAdapter mListAdapter;
    private ArrayList<String> mAl;

    private SimpleDateFormat mDateFormat;

    private ChildEventListener mLogEventListener;
    private ChildEventListener mStateEventListener;
    private ValueEventListener mAvailableListener;


    private DatabaseReference mDatabase;

    private OnFragmentInteractionListener mListener;

    private Query mQuery;
    private DatabaseReference mRef;

    public DoorFragment() {
        // Required empty public constructor
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mAl = new ArrayList<String>();
        mIdSensorAvailable = 0;
        mDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    }


    public static DoorFragment newInstance(String param1) {
        DoorFragment fragment = new DoorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDoorID = getArguments().getString(ARG_PARAM1);
        }
        mKeyNames = ((MainActivity)getActivity()).getKeyNames();
        mMp = MediaPlayer.create(getActivity(), R.raw.door_lock);

        mListAdapter = new MyListAdapter(getActivity(), R.layout.itemlistrow, mAl);

        mLogEventListener = new ChildEventListener() {
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Map<String, Long> data = (Map<String, Long>) dataSnapshot.getValue();

                if (data.containsKey("unlocked"))
                {
                    // otkljucavanje
                    Date dtUnlocked = new Date(data.get("unlocked"));
                    String key = mKeyNames.get(data.get("key"));
                    mAl.add(0, mDateFormat.format(dtUnlocked) + " by " + key);
                }
                else if (data.containsKey("opened"))
                {
                    //otvaranje
                    Date dtOpened = new Date(data.get("opened"));
                    Date dtClosed = null;
                    if (data.containsKey("closed"))
                        dtClosed = new Date(data.get("closed"));

                    String duration;
                    if (dtClosed == null)
                        duration = " and still open.";
                    else {
                        long diffInSec = TimeUnit.MILLISECONDS.toSeconds(dtClosed.getTime() - dtOpened.getTime());
                        duration = " for " + diffInSec + " seconds.";
                    }
                    mAl.add(0, mDateFormat.format(dtOpened) + duration);
                }
                mListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Map<String, Long> data = (Map<String, Long>) dataSnapshot.getValue();

                if (data.containsKey("opened")) {
                    // otvaranje se moze samo promijeniti
                    Date dtOpened = new Date(data.get("opened"));
                    Date dtClosed = new Date(data.get("closed"));

                    long diffInSec = TimeUnit.MILLISECONDS.toSeconds(dtClosed.getTime() - dtOpened.getTime());

                    mAl.remove(0);
                    mAl.add(0, mDateFormat.format(dtOpened) + " for " + diffInSec + " seconds.");

                    mListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {

            }
        };
        mStateEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if ((long)dataSnapshot.getValue() == 0)
                {
                    mImageButtonUnlock.setBackgroundResource(R.drawable.unlock_green);
                }
                else if ((long)dataSnapshot.getValue() == 1)
                {
                    mImageButtonUnlock.setBackgroundResource(R.drawable.unlock_red);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if ((long)dataSnapshot.getValue() == 0)
                {
                    mImageButtonUnlock.setBackgroundResource(R.drawable.unlock_green);
                }
                else if ((long)dataSnapshot.getValue() == 1)
                {
                    mImageButtonUnlock.setBackgroundResource(R.drawable.unlock_red);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {

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


        final View view = inflater.inflate(R.layout.fragment_door, container, false);

        mImageViewAva = (ImageView)view.findViewById(R.id.imageViewAva2);
        mImageButtonUnlock = (ImageButton) view.findViewById(R.id.imageButton);

        ListView listView = (ListView)view.findViewById(R.id.listViewHistory);
        listView.setAdapter(mListAdapter);

        ((ImageButton) view.findViewById(R.id.imageButton)).setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatabase.child(mDoorID).child("Cmd").child("cmdUnlock").setValue(1);
                DatabaseReference ref = mDatabase.child(mDoorID).child("Log").push();

                Map<String, Long> value = new HashMap<String, Long>();
                value.put("unlocked", Calendar.getInstance().getTime().getTime());
                value.put("key", (long)FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode());
                ref.setValue(value);
                mMp.start();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

       mDatabase.child(mDoorID).child("availability").addValueEventListener(mAvailableListener);


        if (mQuery != null)
            mQuery.removeEventListener(mLogEventListener);
        mAl.clear();
        mQuery = mDatabase.child(mDoorID).child("Log").limitToLast(10);
        mQuery.addChildEventListener(mLogEventListener);

        if (mRef != null)
            mRef.removeEventListener(mStateEventListener);
        mRef = mDatabase.child(mDoorID).child("Cmd");
        mRef.addChildEventListener(mStateEventListener);

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("DEBUG", "Stop: " + mDoorID);

        if (mQuery != null)
            mQuery.removeEventListener(mLogEventListener);
        if (mRef != null)
            mRef.removeEventListener(mStateEventListener);
        mDatabase.child(mDoorID).child("availability").removeEventListener(mAvailableListener);
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
