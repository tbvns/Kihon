package xyz.tbvns.kihon.fragments;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.chip.Chip;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_files, container, false);
        LinearLayout layout = view.findViewById(R.id.fileList);
        List<DocumentFile> sortedFiles = sort(files);

        for (DocumentFile file : sortedFiles) {
            if (file.isDirectory()) {
                Button folderButton = new Button(requireContext());
                folderButton.setText(file.getName());
                folderButton.setOnClickListener(v -> {
                    Toast.makeText(requireContext(), "Clicked: " + file.getName(), Toast.LENGTH_SHORT).show();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.main, new FileFragment(Arrays.asList(file.listFiles())))
                            .commit();
                    previousFolders.add(file);
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

        Button exportButton = new Button(requireContext());
        exportButton.setText("Export");
        exportButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.teal_700)));
        layout.addView(exportButton);

        exportButton.setOnClickListener(c -> {
            getFragmentManager().beginTransaction()
                    .replace(R.id.main, new ExportOptions(selectedFiles))
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
}
