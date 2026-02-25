package xyz.tbvns.kihon.ui.manage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import xyz.tbvns.kihon.R;
import xyz.tbvns.kihon.logic.Object.RenderedFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RenderedFilesAdapter extends RecyclerView.Adapter<RenderedFilesAdapter.RenderedFileViewHolder> {

    private final ArrayList<RenderedFile> files;
    private OnFileActionListener listener;

    public interface OnFileActionListener {
        void onOpen(RenderedFile file);
        void onShare(RenderedFile file);
        void onDelete(RenderedFile file, int position);
    }

    public RenderedFilesAdapter(ArrayList<RenderedFile> files, OnFileActionListener listener) {
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RenderedFileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rendered_file_component, parent, false);
        return new RenderedFileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RenderedFileViewHolder holder, int position) {
        RenderedFile file = files.get(position);
        holder.bind(file, position);
    }

    @Override
    public int getItemCount() {
        return files != null ? files.size() : 0;
    }

    public void removeAt(int position) {
        files.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, files.size());
    }

    public class RenderedFileViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView dateView;
        private final TextView sizeView;
        private final Button btnOpen;
        private final Button btnShare;
        private final Button btnDelete;

        private final SimpleDateFormat DATE_FORMAT =
                new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        public RenderedFileViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.rendered_name);
            dateView = itemView.findViewById(R.id.rendered_date);
            sizeView = itemView.findViewById(R.id.rendered_size);
            btnOpen = itemView.findViewById(R.id.btn_open);
            btnShare = itemView.findViewById(R.id.btn_share);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(RenderedFile file, int position) {
            nameView.setText(file.name);
            dateView.setText(DATE_FORMAT.format(new Date(file.lastModified)));
            sizeView.setText(file.getFormattedSize());

            btnOpen.setOnClickListener(v -> {
                if (listener != null) listener.onOpen(file);
            });

            btnShare.setOnClickListener(v -> {
                if (listener != null) listener.onShare(file);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(file, getAdapterPosition());
            });
        }
    }
}