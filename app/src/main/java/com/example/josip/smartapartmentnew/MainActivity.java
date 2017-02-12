package com.example.josip.smartapartmentnew;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

public class MainActivity extends AppCompatActivity implements
        ClimateFragment.OnFragmentInteractionListener,
        DoorFragment.OnFragmentInteractionListener,
        OverviewFragment.OnFragmentInteractionListener{

    public void onFragmentInteraction(Uri uri)
    {
        Log.d("DEBUG", "URI: " + uri.toString());
    }
    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */

    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private FirebaseUser mFirebaseUser;

    private int mSelectedApartment;
    private List<String> mApartmentIDs;
    private List<List<String>> mClimateIDs;
    private List<List<String>> mDoorIDs;
    private Map<String,String> mNames;
    private Map<Long,String> mKeyNames;


    public Map<Long,String> getKeyNames()
    {
        return mKeyNames;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // do your stuff here after SecondActivity finished.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mApartmentIDs = new ArrayList<String>();
        mClimateIDs = new ArrayList<>();
        mDoorIDs = new ArrayList<>();
        mNames = new HashMap<>();
        mKeyNames = new HashMap<>();

        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        int requestCode = 0;
        if (mFirebaseUser == null) {
           startActivityForResult(new Intent(this, LoginActivity.class), requestCode);
        }

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        // informacija je li aplikacija spojena na Firebase service
        database.child(".info").child("connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                if (connected) {
                    fab.setImageResource(R.drawable.connected);
                } else {
                    fab.setImageResource(R.drawable.disconnected);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setImageResource(R.drawable.disconnected);
            }
        });


        // kakvo je stanje s mFirebaseUser ako se startao novi activity?

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

        mSelectedApartment = 0;

        setTitle(mNames.get(mApartmentIDs.get(0)));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);


        for (String str : mApartmentIDs) {
            menu.add(mNames.get(str));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sign_out) {
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        }


        return super.onOptionsItemSelected(item);
    }



    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        ArrayList<String> naslovi = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();


        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);

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
            else
                return OverviewFragment.newInstance("test");
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
    }
}