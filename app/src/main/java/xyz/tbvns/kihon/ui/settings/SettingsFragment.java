package xyz.tbvns.kihon.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import xyz.tbvns.EZConfig;
import xyz.tbvns.kihon.Settings;
import xyz.tbvns.kihon.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private FragmentSettingsBinding binding;

    // Launcher for the folder picker intent
    private final ActivityResultLauncher<Uri> folderPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null) {
                    // Take persistable permission so the URI survives app restarts
                    requireContext().getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );

                    String path = uri.toString();
                    Settings.mihonPath = path;
                    try {
                        EZConfig.save();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    Toast.makeText(requireContext(), "Path: " + path, Toast.LENGTH_LONG).show();
                } else {
                    Log.d(TAG, "Folder selection cancelled");
                }
            });

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Button selectFolderButton = root.findViewById(xyz.tbvns.kihon.R.id.button);
        selectFolderButton.setOnClickListener(v -> folderPickerLauncher.launch(null));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}