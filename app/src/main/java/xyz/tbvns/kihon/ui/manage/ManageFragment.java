package xyz.tbvns.kihon.ui.manage;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import xyz.tbvns.kihon.R;
import xyz.tbvns.kihon.databinding.FragmentManageBinding;
import xyz.tbvns.kihon.logic.FilesLogic;
import xyz.tbvns.kihon.logic.Object.RenderedFile;

import java.util.ArrayList;

public class ManageFragment extends Fragment implements RenderedFilesAdapter.OnFileActionListener {

    private FragmentManageBinding binding;
    private ArrayList<RenderedFile> renderedFiles = new ArrayList<>();
    private RenderedFilesAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentManageBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.renderedRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new RenderedFilesAdapter(renderedFiles, this);
        recyclerView.setAdapter(adapter);

        loadRenderedFiles();

        return root;
    }

    private void loadRenderedFiles() {
        try {
            ArrayList<RenderedFile> loaded = FilesLogic.listRendered();
            renderedFiles.clear();
            renderedFiles.addAll(loaded);
            adapter.notifyDataSetChanged();

            // Show empty state if needed
            boolean isEmpty = renderedFiles.isEmpty();
            if (binding.emptyState != null) {
                binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
            if (binding.renderedRecyclerView != null) {
                binding.renderedRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(RenderedFile file) {
        try {
            Uri uri = file.file.getUri();
            String mimeType = file.extension.equals("pdf") ? "application/pdf" : "application/epub+zip";

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open with"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onShare(RenderedFile file) {
        try {
            Uri uri = file.file.getUri();
            String mimeType = file.extension.equals("pdf") ? "application/pdf" : "application/epub+zip";

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share " + file.name));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDelete(RenderedFile file, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete file")
                .setMessage("Are you sure you want to delete \"" + file.name + "." + file.extension + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = file.file.delete();
                    if (deleted) {
                        adapter.removeAt(position);
                        if (renderedFiles.isEmpty() && binding.emptyState != null) {
                            binding.emptyState.setVisibility(View.VISIBLE);
                            binding.renderedRecyclerView.setVisibility(View.GONE);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}