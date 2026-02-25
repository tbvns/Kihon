package xyz.tbvns.kihon.ui.convert;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import xyz.tbvns.kihon.databinding.ChapterComponentBinding;
import xyz.tbvns.kihon.logic.Object.BrowserItem;
import xyz.tbvns.kihon.logic.Object.ChapterObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder> {
    private final ArrayList<ChapterObject> items;
    private final ArrayList<ChapterObject> selectedChapters;
    private final OnChapterSelectListener listener;

    public interface OnChapterSelectListener {
        void onToggle(ChapterObject chapterObject, boolean isSelected);
    }

    public ChapterAdapter(ArrayList<BrowserItem> items, ArrayList<ChapterObject> selectedChapters, OnChapterSelectListener listener) {
        this.items = items.stream().map(BrowserItem::getChapter).collect(Collectors.toCollection(ArrayList::new));
        this.selectedChapters = selectedChapters;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(ChapterComponentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChapterObject obj = items.get(position);
        String name = obj.title;
        holder.binding.chapterTitle.setText(name);
        holder.binding.chapterCheckbox.setOnCheckedChangeListener(null);
        holder.binding.chapterCheckbox.setChecked(selectedChapters.contains(obj));
        holder.binding.chapterCheckbox.setOnCheckedChangeListener((btn, isChecked) -> {
            listener.onToggle(obj, isChecked);
        });
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