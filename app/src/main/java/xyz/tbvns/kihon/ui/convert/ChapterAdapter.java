package xyz.tbvns.kihon.ui.convert;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import xyz.tbvns.kihon.databinding.ChapterComponentBinding;
import java.util.ArrayList;
import java.util.HashSet;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder> {
    private final ArrayList<String> items;
    private final HashSet<String> selectedChapters;
    private final OnChapterSelectListener listener;

    public interface OnChapterSelectListener {
        void onToggle(String name, boolean isSelected);
    }

    public ChapterAdapter(ArrayList<String> items, HashSet<String> selectedChapters, OnChapterSelectListener listener) {
        this.items = items;
        this.selectedChapters = selectedChapters;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(ChapterComponentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String name = items.get(position);
        holder.binding.chapterTitle.setText(name);

        // 1. Remove listener before setting state to avoid "scroll-firing"
        holder.binding.chapterCheckbox.setOnCheckedChangeListener(null);

        // 2. Set the state based on the Activity's HashSet
        holder.binding.chapterCheckbox.setChecked(selectedChapters.contains(name));

        // 3. Re-add listener
        holder.binding.chapterCheckbox.setOnCheckedChangeListener((btn, isChecked) -> {
            listener.onToggle(name, isChecked);
        });

        // Toggle when clicking the whole row, not just the box
        holder.itemView.setOnClickListener(v -> holder.binding.chapterCheckbox.toggle());
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ChapterComponentBinding binding;
        public ViewHolder(ChapterComponentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}