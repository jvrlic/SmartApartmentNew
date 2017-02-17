package com.example.josip.smartapartmentnew;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;


public class OverviewFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";

    private String mParam1;


    public OverviewFragment() {
        // Required empty public constructor
    }

    public static OverviewFragment newInstance(String param1) {
        OverviewFragment fragment = new OverviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_overview, container, false);

        LinearLayout placeHolder = (LinearLayout)view.findViewById(R.id.climate_space);
        //getLayoutInflater(null).inflate(R.layout.climate_for_overview, placeHolder);
        //getLayoutInflater(null).inflate(R.layout.climate_for_overview, placeHolder);

        return view;
    }
}
