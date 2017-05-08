package com.example.josip.smartapartmentnew;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class OverviewFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String [] mClimateIDs;
    private String [] mDoorIDs;
    private Map<String,String> mNames;
    private Map<Long,String> mKeyNames;

    private List<String> mAl;
    private SimpleDateFormat mDateFormat;

    private DatabaseReference mDatabase;

    private ValueEventListener mDataEventListener;
    private ChildEventListener mLogEventListener;

    private Query mQuery;
    private int lastCount = 10;

    private TextView [] mTextViewClimatesTemp;
    private TextView [] mTextViewClimatesHum;

    private MyListAdapter mListAdapter;

    private ArrayList<DatabaseReference> mAlDataRef;
    private ArrayList<Query> mAlLogRef;

    public OverviewFragment() {
        // Required empty public constructor
        mDatabase = FirebaseDatabase.getInstance().getReference();
        //mAl = new SortedList<String>(String.class, new Comparator());
        mAl = new ArrayList<String>();

        mAlDataRef = new ArrayList<>();
        mAlLogRef = new ArrayList<>();

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
                String ident = mNames.get(dataSnapshot.getRef().getParent().getParent().getKey()).substring(0, 1).toUpperCase();

                if (data.containsKey("unlocked"))
                {
                    // otkljucavanje
                    Date dtUnlocked = new Date(data.get("unlocked"));
                    String key = mKeyNames.get(data.get("key"));
                    mAl.add(0, ident + ":" + mDateFormat.format(dtUnlocked) + " by " + key);
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
                        if (diffInSec <= 60)
                            duration = " for " + diffInSec + " seconds.";
                        else
                            duration = " for " + (int)(diffInSec / 60) + " minutes.";
                    }
                    mAl.add(0, ident + ":" + mDateFormat.format(dtOpened) + duration);
                }

                // sortiraj listu
                Collections.sort(mAl, new java.util.Comparator<String>() {
                    @Override
                    public int compare(String s, String t1) {
                        try {
                            return mDateFormat.parse(t1.substring(2, 21)).compareTo(mDateFormat.parse(s.substring(2, 21)));
                        }
                        catch(ParseException e)
                        {
                            return 0;
                        }
                    }
                });
                mListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Map<String, Long> data = (Map<String, Long>) dataSnapshot.getValue();

                String ident = mNames.get(dataSnapshot.getRef().getParent().getKey()).substring(0, 1).toUpperCase();

                if (data.containsKey("opened")) {
                    // otvaranje se moze samo promijeniti
                    Date dtOpened = new Date(data.get("opened"));
                    Date dtClosed = new Date(data.get("closed"));

                    long diffInSec = TimeUnit.MILLISECONDS.toSeconds(dtClosed.getTime() - dtOpened.getTime());

                    mAl.remove(0);
                    if (diffInSec <= 60)
                        mAl.add(0, ident + ":" + mDateFormat.format(dtOpened) + " for " + diffInSec + " seconds.");
                    else
                        mAl.add(0, ident + ":" + mDateFormat.format(dtOpened) + " for " + (int)(diffInSec / 60) + " minutes.");


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
                        mTextViewClimatesTemp[i].setText(dataSnapshot.child("temperature").getValue(Integer.class).toString());
                        mTextViewClimatesHum[i].setText(dataSnapshot.child("humidity").getValue(Integer.class).toString());
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

        Spinner spinner = (Spinner)view.findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i)
                {
                    case 0: lastCount = 10; break;
                    case 1: lastCount = 100; break;
                    case 2: lastCount = 1000; break;
                }

                for (String s: mDoorIDs)
                    mDatabase.child(s).child("Log").removeEventListener(mLogEventListener);

                mAl.clear();
                for (String s: mDoorIDs) {
                    mQuery = mDatabase.child(s).child("Log").limitToLast(lastCount);
                    mQuery.addChildEventListener(mLogEventListener);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        for (DatabaseReference ref: mAlDataRef) {
            if (ref != null)
                ref.removeEventListener(mDataEventListener);
        }
        for (String s: mClimateIDs) {
            mAlDataRef.add(mDatabase.child(s).child("Data"));
        }
        for (DatabaseReference ref: mAlDataRef) {
            ref.addValueEventListener(mDataEventListener);
        }

        mAl.clear();
        for (Query ref: mAlLogRef) {
            if (ref != null)
                ref.removeEventListener(mLogEventListener);
        }
        for (String s: mDoorIDs) {
            mAlLogRef.add(mDatabase.child(s).child("Log").limitToLast(lastCount));
        }
        for (Query ref: mAlLogRef) {
            ref.addChildEventListener(mLogEventListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        for (DatabaseReference ref: mAlDataRef) {
            if (ref != null)
                ref.removeEventListener(mDataEventListener);
        }

        for (Query ref: mAlLogRef) {
            if (ref != null)
                ref.removeEventListener(mLogEventListener);
        }
    }

}
