package xyz.tbvns.kihon.ui.convert;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import xyz.tbvns.kihon.activity.BooksList;
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
            e.printStackTrace();
        }
    }

    private void onSourceClick(String sourceTitle) {
        Log.i("TAG", "onSourceClick: " + sourceTitle);

        Intent intent = new Intent(getContext(), BooksList.class);
        intent.putExtra("folder_name", sourceTitle);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}