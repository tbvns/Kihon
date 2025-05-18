package xyz.tbvns.kihon.fragments;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import xyz.tbvns.kihon.Config.MainConfig;
import xyz.tbvns.kihon.Constant;
import xyz.tbvns.kihon.FileListAdapter;
import xyz.tbvns.kihon.Formats.EpubUtils;
import xyz.tbvns.kihon.Formats.PdfUtils;
import xyz.tbvns.kihon.R;

import java.io.*;
import java.util.*;

public class FileFragment extends Fragment {
    public static ArrayList<DocumentFile> previousFolders = new ArrayList<>();

    private List<DocumentFile> files;

    public FileFragment(List<DocumentFile> files) {
        this.files = files;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    final HashSet<DocumentFile> selectedFiles = new HashSet<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);


        Button exportButton = view.findViewById(R.id.export);
        exportButton.setOnClickListener(c -> {
            if (!selectedFiles.isEmpty()) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.main, new ExportOptions(selectedFiles, MainConfig.reEncodeByDefault))
                        .addToBackStack("files")
                        .commit();
            } else {
                Toast.makeText(getContext(), "No chapter selected !", Toast.LENGTH_LONG).show();
            }

        });

        RecyclerView layout = view.findViewById(R.id.filesView);
        layout.setLayoutManager(new LinearLayoutManager(getContext()));
        layout.setItemViewCacheSize(10);
        layout.setHasFixedSize(true);

        FileListAdapter adapter = new FileListAdapter(
                requireContext(),
                sort(files),
                selectedFiles,
                folder -> {
                    // onFolderClick
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main, new FileFragment(Arrays.asList(folder.listFiles())))
                            .addToBackStack("files")
                            .commit();
                    previousFolders.add(folder);
                    selectedFiles.clear();
                }
        );

        layout.setAdapter(adapter);

        return view;
    }

    public static List<DocumentFile> sort(List<DocumentFile> doc) {
        List<DocumentFile> unsorted = new ArrayList<>();
        HashMap<Integer, DocumentFile> filesID = new HashMap<>();
        for (DocumentFile file : doc) {
            try {
                String[] split = file.getUri().getPath().toString().split("/");
                int id = Integer.parseInt(split[split.length-1].replaceAll("\\D", "").replace(".", "").strip());
                filesID.put(id, file);
            } catch (Exception e) {
                System.err.println("Error:" + e.getMessage() + ": " + file.getUri().getPath());
                unsorted.add(file);
            }
        }
        List<DocumentFile> sortedFiles = new ArrayList<>();
        filesID.keySet().stream().sorted().forEach(id -> {
            sortedFiles.add(filesID.get(id));
        });
        sortedFiles.addAll(unsorted);
        return sortedFiles;
    }

    public static MaterialButton createStyledButton(Context context) {
        MaterialButton button = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonStyle);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        button.setLayoutParams(params);

        button.setCornerRadius(16);
        button.setElevation(4f);

        button.setBackgroundTintList(ContextCompat.getColorStateList(context, com.google.android.material.R.color.material_dynamic_neutral40));

        button.setTextColor(Color.WHITE);
        button.setTextSize(16);

        return button;
    }
}
