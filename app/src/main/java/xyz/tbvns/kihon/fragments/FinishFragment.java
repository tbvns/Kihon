package xyz.tbvns.kihon.fragments;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import xyz.tbvns.kihon.R;
import xyz.tbvns.kihon.ShareUtils;

public class FinishFragment extends Fragment {

    DocumentFile pdf;
    public FinishFragment(DocumentFile pdf) {
        this.pdf = pdf;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_finish, container, false);
        if (pdf != null) {
            Button sharebutton = view.findViewById(R.id.sharebutton);
            sharebutton.setOnClickListener(v -> {
                ShareUtils.sharePdfFile(getContext(), pdf);
            });
            Button openbutton = view.findViewById(R.id.openbutton);
            openbutton.setOnClickListener(v -> {
                ShareUtils.openPdfFile(getContext(), pdf);
            });
            TextView fileName = view.findViewById(R.id.filename);
            fileName.setText("Exported \"" + pdf.getName() + "\"");
        } else {
            TextView fileName = view.findViewById(R.id.filename);
            TextView title = view.findViewById(R.id.titleexported);
            title.setText("Failed !");
            fileName.setText("Could not export as pdf. Try again !");
            LinearLayout layout = view.findViewById(R.id.finishedLayout);
            LinearLayout buttons = view.findViewById(R.id.finishbuttons);
            layout.removeView(buttons);
        }
        return view;
    }
}