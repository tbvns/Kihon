package xyz.tbvns.kihon.ui.convert;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import xyz.tbvns.kihon.R;

import java.util.ArrayList;

public class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.SourceViewHolder> {

    private ArrayList<String> sources;
    private OnSourceClickListener onSourceClickListener;

    public interface OnSourceClickListener {
        void onSourceClick(String sourceTitle);
    }

    public SourceAdapter(ArrayList<String> sources, OnSourceClickListener onSourceClickListener) {
        this.sources = sources;
        this.onSourceClickListener = onSourceClickListener;
    }

    @NonNull
    @Override
    public SourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.source_component, parent, false);
        return new SourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SourceViewHolder holder, int position) {
        String sourceTitle = sources.get(position);
        holder.bind(sourceTitle, onSourceClickListener);
    }

    @Override
    public int getItemCount() {
        return sources != null ? sources.size() : 0;
    }

    public static class SourceViewHolder extends RecyclerView.ViewHolder {
        private TextView titleView;

        public SourceViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.source_title);
        }

        public void bind(String sourceTitle, OnSourceClickListener onSourceClickListener) {
            titleView.setText(sourceTitle);
            itemView.setOnClickListener(v -> {
                if (onSourceClickListener != null) {
                    onSourceClickListener.onSourceClick(sourceTitle);
                }
            });
        }
    }
}