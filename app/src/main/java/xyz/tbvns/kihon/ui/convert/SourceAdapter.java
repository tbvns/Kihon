package xyz.tbvns.kihon.ui.convert;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import xyz.tbvns.kihon.R;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SourceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<String> sources;
    private OnSourceClickListener onSourceClickListener;

    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_CHAPTER = 1;

    private final Set<Integer> selectedPositions = new HashSet<>();

    public interface OnSourceClickListener {
        void onSourceClick(String sourceTitle);
    }

    public SourceAdapter(ArrayList<String> sources, OnSourceClickListener onSourceClickListener) {
        this.sources = sources;
        this.onSourceClickListener = onSourceClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        // Detect if it's a chapter or a folder based on extension
        if (sources.get(position).toLowerCase().endsWith(".cbz")) {
            return TYPE_CHAPTER;
        }
        return TYPE_FOLDER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CHAPTER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chapter_component, parent, false);
            return new ChapterViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.source_component, parent, false);
            return new SourceViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        String title = sources.get(position);
        if (holder instanceof ChapterViewHolder) {
            ((ChapterViewHolder) holder).bind(title, position);
        } else if (holder instanceof SourceViewHolder) {
            ((SourceViewHolder) holder).bind(title, onSourceClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return sources != null ? sources.size() : 0;
    }

    // FOLDER VIEW HOLDER
    public static class SourceViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;

        public SourceViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.source_title);
        }

        public void bind(String sourceTitle, OnSourceClickListener listener) {
            titleView.setText(sourceTitle);

            // Restoration of original click behavior:
            // Setting the listener on both the view and the title to ensure it fires
            View.OnClickListener clickListener = v -> {
                if (listener != null) {
                    listener.onSourceClick(sourceTitle);
                }
            };

            itemView.setOnClickListener(clickListener);
            if (titleView != null) {
                titleView.setOnClickListener(clickListener);
            }
        }
    }

    // CHAPTER VIEW HOLDER
    public class ChapterViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final CheckBox checkBox;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.chapter_title);
            checkBox = itemView.findViewById(R.id.chapter_checkbox);
        }

        public void bind(String title, int position) {
            titleView.setText(title);

            boolean isSelected = selectedPositions.contains(position);
            itemView.setSelected(isSelected);
            if (checkBox != null) checkBox.setChecked(isSelected);

            itemView.setOnClickListener(v -> {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position);
                } else {
                    selectedPositions.add(position);
                }
                notifyItemChanged(position);
            });
        }
    }
}