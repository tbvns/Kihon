package xyz.tbvns.kihon.component;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import xyz.tbvns.kihon.R;

public class SourceComponent extends Fragment {

    private static final String ARG_TITLE = "title";

    private String mTitle;
    private Runnable mOnClick;

    public SourceComponent() {}

    public static SourceComponent newInstance(String title, Runnable onClick) {
        SourceComponent fragment = new SourceComponent();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        fragment.mOnClick = onClick;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(ARG_TITLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.source_component, container, false);

        TextView titleView = view.findViewById(R.id.source_title);
        if (titleView != null) {
            titleView.setText(mTitle);
        }

        view.setOnClickListener(v -> {
            if (mOnClick != null) {
                mOnClick.run();
            }
        });

        return view;
    }
}