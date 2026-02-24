package xyz.tbvns.kihon.ui.convert;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import xyz.tbvns.kihon.databinding.FragmentConvertBinding;
import xyz.tbvns.kihon.logic.FilesLogic;

import java.util.ArrayList;

public class ConvertFragment extends Fragment {

    private FragmentConvertBinding binding;
    private ArrayList<String> sources = new ArrayList<>();
    private SourceAdapter sourceAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ConvertViewModel convertViewModel =
                new ViewModelProvider(this).get(ConvertViewModel.class);

        binding = FragmentConvertBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup RecyclerView with empty list first
        RecyclerView recyclerView = binding.sourcesRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        sourceAdapter = new SourceAdapter(sources, this::onSourceClick);
        recyclerView.setAdapter(sourceAdapter);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load sources after view is created (safer lifecycle point)
        loadSources();
    }

    private void loadSources() {
        try {
            ArrayList<String> loadedSources = FilesLogic.listSources();
            if (loadedSources != null && !loadedSources.isEmpty()) {
                sources.clear();
                sources.addAll(loadedSources);
                sourceAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            // Handle any errors gracefully
            e.printStackTrace();
        }
    }

    private void onSourceClick(String sourceTitle) {
        // Handle source item click here
        // For example: navigate to detail screen, show dialog, etc.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}