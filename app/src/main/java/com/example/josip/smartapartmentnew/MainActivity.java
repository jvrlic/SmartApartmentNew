package com.example.josip.smartapartmentnew;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity{

    private ViewPager mViewPager;

    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;
    private DatabaseReference mConRef;

    private boolean mFirstShow;
    private int mSelectedApartment;
    private List<String> mApartmentIDs;
    private List<List<String>> mClimateIDs;
    private List<List<String>> mDoorIDs;
    private Map<String,String> mNames;
    private Map<Long,String> mKeyNames;

    private ValueEventListener connectedListener;

    public Map<Long,String> getKeyNames()
    {
        return mKeyNames;
    }
    public Map<String,String> getNames()
    {
        return mNames;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // do your stuff here after SecondActivity finished.
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //View decorView = getWindow().getDecorView();
        //int uiOptinos = View.SYSTEM_UI_FLAG_FULLSCREEN;
        //decorView.setSystemUiVisibility(uiOptinos);


        mApartmentIDs = new ArrayList<String>();
        mClimateIDs = new ArrayList<>();
        mDoorIDs = new ArrayList<>();
        mNames = new HashMap<>();
        mKeyNames = new HashMap<>();

        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        int requestCode = 0;
        if (mFirebaseUser == null) {
            startActivityForResult(new Intent(this, LoginActivity.class), requestCode);
        }

        mViewPager = (ViewPager) findViewById(R.id.container);
        ((TabLayout) findViewById(R.id.tabs)).setupWithViewPager(mViewPager);

        connectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = dataSnapshot.getValue(Boolean.class);
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                if (connected) {
                    fab.setImageResource(R.drawable.connected);
                } else {
                    fab.setImageResource(R.drawable.disconnected);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setImageResource(R.drawable.disconnected);
            }
        };

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSelectedApartment = 0;
        mFirstShow = true;

/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mFirebaseUser != null) {
            // informacija je li aplikacija spojena na Firebase service
            if (connectedListener != null) {
                if (mConRef != null)
                    mConRef.removeEventListener(connectedListener);
                mConRef = mDatabase.child(".info").child("connected");
                mConRef.addValueEventListener(connectedListener);
            }

            // ako je dostupan firebase i samo jednom citaj preference i postavi tabove
            if (mFirstShow == true){
                ReadPreferences();

                mViewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));

                mFirstShow = false;
            }

            if (mApartmentIDs.size() != 0){
                setTitle(mNames.get(mApartmentIDs.get(mSelectedApartment)));
            }
            else{
                setTitle("No apartments");
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mConRef != null)
            mConRef.removeEventListener(connectedListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        for (String str : mApartmentIDs) {
            menu.add(0, mApartmentIDs.indexOf(str), 0, mNames.get(str));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_sign_out) {
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        }
        else {
            mSelectedApartment = item.getItemId();

            setTitle(mNames.get(mApartmentIDs.get(mSelectedApartment)));

            //mViewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));

            ((SectionsPagerAdapter)mViewPager.getAdapter()).setPages();
            ((SectionsPagerAdapter)mViewPager.getAdapter()).notifyChangeInPosition(3);
            mViewPager.getAdapter().notifyDataSetChanged();
        }

        return super.onOptionsItemSelected(item);
    }


    private void ReadPreferences() {
        SharedPreferences prefs = this.getSharedPreferences("PREF_PERSONAL_DATA", Context.MODE_PRIVATE);

        int count = Integer.parseInt(prefs.getString("numberOfApartments", "0"));
        for (int i = 0; i < count; i++) {
            String id = prefs.getString("apartment".concat(Integer.toString(i + 1)), "");
            mApartmentIDs.add(id);
            mNames.put(id, prefs.getString(id.concat("_name"), ""));

            int nClimates = Integer.parseInt(prefs.getString(id.concat("_climate_count") , "0"));
            List<String> climatesTemp = new ArrayList<>();
            for (int j = 1; j <= nClimates; j++) {
                String idClimate = prefs.getString(id.concat("_climate_id").concat(Integer.toString(j)), "climate_".concat(Integer.toString(i * 10 + j)));
                climatesTemp.add(idClimate);
                mNames.put(idClimate, prefs.getString(id.concat("_climate_name").concat(Integer.toString(j)), "climate_".concat(Integer.toString(i * 10 + j))));
            }
            mClimateIDs.add(climatesTemp);

            int nDoors = Integer.parseInt(prefs.getString(id.concat("_door_count") , "0"));
            List<String> doorsTemp = new ArrayList<>();
            for (int j = 1; j <= nDoors; j++) {
                String idDoor = prefs.getString(id.concat("_door_id").concat(Integer.toString(j)), "door_".concat(Integer.toString(i * 10 + j)));
                doorsTemp.add(idDoor);
                mNames.put(idDoor, prefs.getString(id.concat("_door_name").concat(Integer.toString(j)), "door_".concat(Integer.toString(i * 10 + j))));
            }
            mDoorIDs.add(doorsTemp);

            int nKeys = Integer.parseInt(prefs.getString(id.concat("_key_count") , "0"));
            for (int j = 1; j <= nKeys; j++) {
                String idKey = prefs.getString(id.concat("_key_id").concat(Integer.toString(j)), "key_".concat(Integer.toString(i * 10 + j)));
                mKeyNames.put(Long.parseLong(idKey), prefs.getString(id.concat("_key_name").concat(Integer.toString(j)), "key_".concat(Integer.toString(i * 10 + j))));
            }
        }
    }



    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<String> naslovi = new ArrayList<>();
        private ArrayList<String> ids = new ArrayList<>();

        private long mBaseId = 0;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);

            setPages();
        }

        public void setPages()
        {
            naslovi.clear();
            ids.clear();

            naslovi.add(getResources().getString(R.string.tab_overview_name));
            ids.add("0");
            for (String str : mDoorIDs.get(mSelectedApartment)) {
                naslovi.add(mNames.get(str));
                ids.add(str);
            }
            for (String str : mClimateIDs.get(mSelectedApartment)) {
                naslovi.add(mNames.get(str));
                ids.add(str);
            }
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            String id = ids.get(position);
            if (id.contains("Climate"))
                return ClimateFragment.newInstance(id);
            else if (id.contains("Door"))
                return DoorFragment.newInstance(id);
            else {
                return OverviewFragment.newInstance((String[])mClimateIDs.get(mSelectedApartment).toArray(new String[0]), (String[])mDoorIDs.get(mSelectedApartment).toArray(new String[0]));
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return naslovi.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return naslovi.get(position);
        }


        @Override
        public int getItemPosition(Object object) {
            // refresh all fragments when data set changed
            return PagerAdapter.POSITION_NONE;
        }
        @Override
        public long getItemId(int position) {
            // give an ID different from position when position has been changed
            return mBaseId + position;
        }

        public void notifyChangeInPosition(int n) {
            // shift the ID returned by getItemId outside the range of all previous fragments
            mBaseId += getCount() + n;
        }

    }
}