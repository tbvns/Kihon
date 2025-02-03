package xyz.tbvns.kihon;

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

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

public class FileFragment extends Fragment {
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

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                Button folderButton = new Button(requireContext());
                folderButton.setText(file.getName());
                folderButton.setOnClickListener(v -> {
                    Toast.makeText(requireContext(), "Clicked: " + file.getName(), Toast.LENGTH_SHORT).show();

                    getFragmentManager().beginTransaction()
                            .replace(R.id.main, new FileFragment(Arrays.asList(file.listFiles())))
                            .commit();
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
        exportButton.setText("Export as pdf");
        exportButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.teal_700)));
        layout.addView(exportButton);

        exportButton.setOnClickListener(c -> {
            Context context = requireContext();
            FragmentManager manager = getParentFragmentManager();

            new Thread(() -> {
                manager.beginTransaction()
                        .replace(R.id.main, new LoadingFragment())
                        .commit();

                List<DocumentFile> pngs = new ArrayList<>();

                for (DocumentFile file : selectedFiles) {
                    DocumentFile e = extractZip(context, file);
                    if (e!=null) {
                        for (DocumentFile listFile : e.listFiles()) {
                            if (listFile.isFile()) {
                                pngs.add(listFile);
                            }
                        }
                    }
                }

                DocumentFile pdf = PdfUtils.createPdfFromPngs(context, pngs, files.get(0).getParentFile().getName() + " from " + files.get(0).getName() + " to " + files.get(files.size()-1).getName());

                new Handler(Looper.getMainLooper()).post(() -> {
                    manager.beginTransaction()
                            .replace(R.id.main, new FinishFragment(pdf))
                            .commit();
                });
            }).start();
        });
        return view;
    }

    private DocumentFile extractZip(Context context, DocumentFile zipFile) {
        try {
            // Step 1: Copy CBZ file to cache directory
            File tempZipFile = new File(context.getCacheDir(), zipFile.getName());
            InputStream inputStream = context.getContentResolver().openInputStream(zipFile.getUri());
            FileOutputStream fileOutputStream = new FileOutputStream(tempZipFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            fileOutputStream.close();

            // Step 2: Open ZipFile using the copied file
            ZipFile zip = new ZipFile(tempZipFile);

            // Step 3: Create extraction folder (same name as CBZ without extension)
            String zipFileName = zipFile.getName();
            if (zipFileName == null) return null;
            if (zipFileName.endsWith(".zip") || zipFileName.endsWith(".cbz")) {
                zipFileName = zipFileName.substring(0, zipFileName.length() - 4);
            }

            DocumentFile extractFolder = Constant.ExtractedFile.createDirectory(zipFileName);
            if (extractFolder == null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Failed to create extraction folder!", Toast.LENGTH_SHORT).show();
                });
                return null;
            }

            // Step 4: Extract Images
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    extractFolder.createDirectory(entry.getName());
                } else {
                    // Only extract images (PNG, JPG, JPEG)
                    if (entry.getName().endsWith(".png") || entry.getName().endsWith(".jpg") || entry.getName().endsWith(".jpeg")) {
                        DocumentFile newFile = extractFolder.createFile("image/*", entry.getName());
                        if (newFile != null) {
                            OutputStream outputStream = context.getContentResolver().openOutputStream(newFile.getUri());
                            InputStream zipInputStream = zip.getInputStream(entry);

                            while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }

                            zipInputStream.close();
                            outputStream.close();
                        }
                    }
                }
            }

            zip.close();

            // Step 5: Delete temporary zip file
            tempZipFile.delete();

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "ZIP Extracted to " + extractFolder.getUri(), Toast.LENGTH_SHORT).show();
            });
            return extractFolder;
        } catch (IOException e) {
            Log.e("BlankFragment", "Error extracting ZIP file", e);
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "Failed to extract ZIP file!", Toast.LENGTH_SHORT).show();
            });
        }
        return null;
    }
}
