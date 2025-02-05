package xyz.tbvns.kihon.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.w3c.dom.Text;
import xyz.tbvns.kihon.R;

public class LoadingFragment extends Fragment {
    public static float progress;
    public static String message;
    public LoadingFragment() {
        progress = 0;
        message = "Starting...";
    }

    Thread thread;
    boolean run = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loading, container, false);
        TextView desc = view.findViewById(R.id.loadingDesk);
        ProgressBar bar = view.findViewById(R.id.loadingBar);

        thread = new Thread(() -> {
            while (run) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    desc.setText(message);
                    bar.setProgress(Math.round(progress));
                });
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        return view;
    }

    @Override
    public void onDestroyView() {
        run = false;
        super.onDestroyView();
    }
}