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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import xyz.tbvns.kihon.Config.MainConfig;
import xyz.tbvns.kihon.Constant;
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

    final List<DocumentFile> selectedFiles = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);
        LinearLayout layout = view.findViewById(R.id.fileList);
        List<DocumentFile> sortedFiles = sort(files);

        for (DocumentFile file : sortedFiles) {
            if (file.isDirectory()) {
                Button folderButton = createStyledButton(getContext());
                folderButton.setText(file.getName());
                folderButton.setOnClickListener(v -> {
                    Toast.makeText(requireContext(), "Clicked: " + file.getName(), Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main, new FileFragment(Arrays.asList(file.listFiles())))
                            .commit();
                    previousFolders.add(file);
                    selectedFiles.clear();
                });

                layout.addView(folderButton);
            } else if (file.isFile()) {
                Chip fileChip = new Chip(requireContext());
                fileChip.setCheckable(true);
                fileChip.setText(file.getName());
                fileChip.setOnClickListener(v -> {
                    if (selectedFiles.contains(file)) {
                        selectedFiles.remove(file);
                    } else {
                        selectedFiles.add(file);
                    }
                });
                layout.addView(fileChip);
            }
        }

        Button exportButton = createStyledButton(getContext());
        exportButton.setText("Export");
        exportButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.teal_700)));
        layout.addView(exportButton);

        exportButton.setOnClickListener(c -> {
            getFragmentManager().beginTransaction()
                    .replace(R.id.main, new ExportOptions(selectedFiles, MainConfig.reEncodeByDefault))
                    .commit();
        });
        return view;
    }

    public static List<DocumentFile> sort(List<DocumentFile> doc) {
        List<DocumentFile> unsorted = new ArrayList<>();
        HashMap<Integer, DocumentFile> filesID = new HashMap<>();
        for (DocumentFile file : doc) {
            try {
                int id = Integer.parseInt(file.getName().replaceAll("\\D", "").replace(".", "").strip());
                filesID.put(id, file);
            } catch (Exception e) {
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
