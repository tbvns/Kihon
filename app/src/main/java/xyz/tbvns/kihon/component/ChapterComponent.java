package xyz.tbvns.kihon.component;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import xyz.tbvns.kihon.R;

public class ChapterComponent extends Fragment {
    private String title;
    private boolean isSelected = false;
    private CheckBox checkbox;
    private OnChapterSelectListener listener;

    public interface OnChapterSelectListener {
        void onChapterSelected(String title, boolean isSelected);
    }

    public static ChapterComponent newInstance(String title) {
        ChapterComponent fragment = new ChapterComponent();
        fragment.title = title;
        return fragment;
    }

    public void setOnChapterSelectListener(OnChapterSelectListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle saved) {
        View view = inflater.inflate(R.layout.chapter_component, container, false);

        TextView titleView = view.findViewById(R.id.chapter_title);
        checkbox = view.findViewById(R.id.chapter_checkbox);

        titleView.setText(title);
        checkbox.setChecked(isSelected);

        checkbox.setOnCheckedChangeListener((buttonView, checked) -> {
            isSelected = checked;
            if (listener != null) {
                listener.onChapterSelected(title, checked);
            }
        });

        return view;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
        if (checkbox != null) {
            checkbox.setChecked(isSelected);
        }
    }

    public boolean isSelected() {
        return isSelected;
    }

    public String getTitle() {
        return title;
    }
}