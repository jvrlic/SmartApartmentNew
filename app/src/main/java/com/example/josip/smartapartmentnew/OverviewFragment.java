package com.example.josip.smartapartmentnew;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class OverviewFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String [] mClimateIDs;
    private String [] mDoorIDs;
    private Map<String,String> mNames;
    private Map<Long,String> mKeyNames;

    private ArrayList<String> mAl;
    private SimpleDateFormat mDateFormat;

    private DatabaseReference mDatabase;

    private ValueEventListener mDataEventListener;
    private ChildEventListener mLogEventListener;

    private Query mQuery;

    private TextView [] mTextViewClimatesTemp;
    private TextView [] mTextViewClimatesHum;

    private MyListAdapter mListAdapter;

    public OverviewFragment() {
        // Required empty public constructor
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAl = new ArrayList<String>();
        mDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    }

    public static OverviewFragment newInstance(String [] param1, String [] param2)  {
        OverviewFragment fragment = new OverviewFragment();
        Bundle args = new Bundle();
        args.putStringArray(ARG_PARAM1, param1);
        args.putStringArray(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mClimateIDs = getArguments().getStringArray(ARG_PARAM1);
            mDoorIDs = getArguments().getStringArray(ARG_PARAM2);
        }

        mNames = ((MainActivity)getActivity()).getNames();
        mKeyNames = ((MainActivity)getActivity()).getKeyNames();

        mTextViewClimatesTemp = new TextView[mClimateIDs.length];
        mTextViewClimatesHum = new TextView[mClimateIDs.length];

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

        mDataEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (int i = 0; i < mClimateIDs.length; i++)
                {
                    if (dataSnapshot.getRef().getParent().getKey().equals(mClimateIDs[i]))
                    {
                        mTextViewClimatesTemp[i].setText(dataSnapshot.child("temperature").getValue().toString());
                        mTextViewClimatesHum[i].setText(dataSnapshot.child("humidity").getValue().toString());
                    }
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

        View view = inflater.inflate(R.layout.fragment_overview, container, false);

        LinearLayout placeHolder = (LinearLayout)view.findViewById(R.id.climate_space);

        View v;
        LayoutInflater li = getLayoutInflater(null);
        for (int i = 0; i < mClimateIDs.length; i++)
        {
            v = li.inflate(R.layout.climate_for_overview, null);
            placeHolder.addView(v);
            ((TextView)v.findViewById(R.id.textViewNaslov)).setText(mNames.get(mClimateIDs[i]));
            mTextViewClimatesTemp[i] = (TextView)v.findViewById(R.id.textViewTempO);
            mTextViewClimatesHum[i] = (TextView)v.findViewById(R.id.textViewHumO);
        }

        ListView listView = (ListView)view.findViewById(R.id.listViewDogadaja);
        listView.setAdapter(mListAdapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        for (String s: mClimateIDs) {
            mDatabase.child(s).child("Data").addValueEventListener(mDataEventListener);
        }

        mAl.clear();
        for (String s: mDoorIDs) {
            mQuery = mDatabase.child(s).child("Log").limitToLast(10);
            mQuery.addChildEventListener(mLogEventListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        for (String s: mClimateIDs)
            mDatabase.child(s).child("Data").removeEventListener(mDataEventListener);

        if (mQuery != null)
            mQuery.removeEventListener(mLogEventListener);
    }

}
