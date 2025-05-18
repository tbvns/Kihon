package xyz.tbvns.kihon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Set;

public class FileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<DocumentFile> files;
    private final Set<DocumentFile> selectedFiles;
    private final Context context;
    private final OnFolderClickListener onFolderClick;

    public interface OnFolderClickListener {
        void onFolderClicked(DocumentFile folder);
    }

    public FileListAdapter(Context context, List<DocumentFile> files, Set<DocumentFile> selectedFiles, OnFolderClickListener onFolderClick) {
        this.context = context;
        this.files = files;
        this.selectedFiles = selectedFiles;
        this.onFolderClick = onFolderClick;
    }

    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_FILE = 1;

    @Override
    public int getItemViewType(int position) {
        return files.get(position).isDirectory() ? TYPE_FOLDER : TYPE_FILE;
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_FOLDER) {
            View view = inflater.inflate(R.xml.item_folder_button, parent, false);
            return new FolderViewHolder(view);
        } else {
            View view = inflater.inflate(R.xml.item_file_chip, parent, false);
            return new FileViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DocumentFile file = files.get(position);
        if (getItemViewType(position) == TYPE_FOLDER) {
            FolderViewHolder vh = (FolderViewHolder) holder;
            vh.button.setText(file.getName());
            vh.button.setOnClickListener(v -> onFolderClick.onFolderClicked(file));
        } else {
            FileViewHolder vh = (FileViewHolder) holder;
            vh.chip.setText(file.getName());
            vh.chip.setChecked(selectedFiles.contains(file));
            vh.chip.setOnClickListener(v -> {
                if (selectedFiles.contains(file)) {
                    selectedFiles.remove(file);
                } else {
                    selectedFiles.add(file);
                }
            });
        }
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        Button button;
        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            button = (Button) itemView;
        }
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        Chip chip;
        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            chip = (Chip) itemView;
        }
    }
}
