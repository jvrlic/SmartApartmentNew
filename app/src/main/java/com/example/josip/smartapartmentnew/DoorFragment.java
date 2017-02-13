package com.example.josip.smartapartmentnew;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";

    private static final String JOSKAN_DOO="TEST1222";
    private static final String ANTIFA_DOO="TEST1222";

    // TODO: Rename and change types of parameters
    private String mDoorID;

    private Map<Long, String> mKeyNames;

    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> al;

    private SimpleDateFormat dateFormat;

    private ChildEventListener logEventListener;
    private ChildEventListener stateEventListener;


    private DatabaseReference mDatabase;

    private OnFragmentInteractionListener mListener;

    private Query mQuery;
    private DatabaseReference mRef;

    public DoorFragment() {
        // Required empty public constructor
        mDatabase = FirebaseDatabase.getInstance().getReference();

        al = new ArrayList<String>();

        dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");


        logEventListener = new ChildEventListener() {
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Map<String, Long> data = (Map<String, Long>) dataSnapshot.getValue();

                if (data.containsKey("unlocked"))
                {
                    // otkljucavanje
                    Date dtUnlocked = new Date(data.get("unlocked"));
                    String key = mKeyNames.get(data.get("key"));
                    al.add(0, dateFormat.format(dtUnlocked) + " by " + key);
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
                    al.add(0, dateFormat.format(dtOpened) + duration);
                }
                listAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Map<String, Long> data = (Map<String, Long>) dataSnapshot.getValue();

                if (data.containsKey("opened")) {
                    // otvaranje se moze samo promijeniti
                    Date dtOpened = new Date(data.get("opened"));
                    Date dtClosed = new Date(data.get("closed"));

                    long diffInSec = TimeUnit.MILLISECONDS.toSeconds(dtClosed.getTime() - dtOpened.getTime());

                    al.remove(0);
                    al.add(0, dateFormat.format(dtOpened) + " for " + diffInSec + " seconds.");

                    listAdapter.notifyDataSetChanged();
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
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment DoorFragment.
     */
    // TODO: Rename and change types and number of parameters
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        final MediaPlayer mp = MediaPlayer.create(this.getActivity(), R.raw.door_lock);

        final View view = inflater.inflate(R.layout.fragment_door, container, false);
        ListView listView = (ListView)view.findViewById(R.id.listViewHistory);

        listAdapter = new ArrayAdapter<String>(this.getActivity(), R.layout.simplerow, al);
        listView.setAdapter(listAdapter);

        stateEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if ((long)dataSnapshot.getValue() == 0)
                {
                    ((ImageButton) view.findViewById(R.id.imageButton)).setBackgroundResource(R.drawable.unlock_green);
                }
                else if ((long)dataSnapshot.getValue() == 1)
                {
                    ((ImageButton) view.findViewById(R.id.imageButton)).setBackgroundResource(R.drawable.unlock_red);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if ((long)dataSnapshot.getValue() == 0)
                {
                    ((ImageButton) view.findViewById(R.id.imageButton)).setBackgroundResource(R.drawable.unlock_green);
                }
                else if ((long)dataSnapshot.getValue() == 1)
                {
                    ((ImageButton) view.findViewById(R.id.imageButton)).setBackgroundResource(R.drawable.unlock_red);
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

        if (mQuery != null)
            mQuery.removeEventListener(logEventListener);
        al.clear();
        mQuery = mDatabase.child(mDoorID).child("Log").limitToLast(10);
        mQuery.addChildEventListener(logEventListener);

        if (mRef != null)
            mRef.removeEventListener(stateEventListener);
        mRef = mDatabase.child(mDoorID).child("Cmd");
        mRef.addChildEventListener(stateEventListener);

        ((ImageButton) view.findViewById(R.id.imageButton)).setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatabase.child(mDoorID).child("Cmd").child("cmdUnlock").setValue(1);
                DatabaseReference ref = mDatabase.child(mDoorID).child("Log").push();

                Map<String, Long> value = new HashMap<String, Long>();
                value.put("unlocked", Calendar.getInstance().getTime().getTime());
                value.put("key", (long)FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode());
                ref.setValue(value);
                mp.start();
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
