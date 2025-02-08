package xyz.tbvns.kihon.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.FragmentManager;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import xyz.tbvns.kihon.Constant;
import xyz.tbvns.kihon.Formats.EpubUtils;
import xyz.tbvns.kihon.Formats.PdfUtils;
import xyz.tbvns.kihon.R;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

@AllArgsConstructor
public class ExportOptions extends Fragment {
    private List<DocumentFile> selectedFiles;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_export_options, container, false);

        String[] formats = {"Electronic Publication (ePUB)", "Portable Document Format (PDF)"/*, "Comic Book Zip (CBZ)"*/};

        Spinner formatSpinner = view.findViewById(R.id.formatSpinner);
        ArrayAdapter<Object> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, formats);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        formatSpinner.setAdapter(adapter);

        Button button = view.findViewById(R.id.Proceed);
        button.setOnClickListener(c -> {
            if (formatSpinner.getSelectedItem().equals("Electronic Publication (ePUB)")) {
                if (selectedFiles.size() >= 5) {
                    new warningDialog(
                            (dialog, which) -> generate(1),
                            (dialog, which) -> generate(0)
                    ).show(getParentFragmentManager(), "warningMemory");
                } else {
                    generate(0);
                }
            } else if (formatSpinner.getSelectedItem().equals("Portable Document Format (PDF)")) {
                generate(1);
            }
        });
        return view;
    }

    @SneakyThrows
    private DocumentFile extractZip(Context context, DocumentFile zipFile) {
        try {
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

    public static int getAsNumber(String str) {
        if (str == null) {
            return -1;
        }
        return Integer.parseInt(str.replaceAll("\\D", "").replace(".", "").strip());
    }

    @AllArgsConstructor
    public static class warningDialog extends DialogFragment {
        private DialogInterface.OnClickListener first;
        private DialogInterface.OnClickListener second;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction.
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Using ePUB files with a large number of chapters may lead to crashes due to excessive memory consumption. To avoid this issue, consider using PDF format instead.")
                    .setTitle("Waning")
                    .setNegativeButton("Continue", first)
                    .setPositiveButton("Switch to PDF", second);
            // Create the AlertDialog object and return it.
            return builder.create();
        }
    }

    public void generate(int type) {
        Context context = requireContext();
        FragmentManager manager = getParentFragmentManager();

        new Thread(() -> {
            manager.beginTransaction()
                    .replace(R.id.main, new LoadingFragment())
                    .commit();

            List<DocumentFile> pngs = new ArrayList<>();
            int max = selectedFiles.size();
            float percent = 0;
            for (DocumentFile file : sort(selectedFiles)) {
                LoadingFragment.message = "Extracting: " + file.getName();
                DocumentFile e = extractZip(context, file);
                if (e != null) {
                    for (DocumentFile listFile : sort(List.of(e.listFiles()))) {
                        System.out.println(e.getName());
                        if (listFile.isFile()) {
                            pngs.add(listFile);
                        }
                    }
                }
                percent += (float) 1 / max * 100;
                LoadingFragment.progress = percent / 2;
            }

            String name = selectedFiles.get(0).getParentFile().getName() + " Ch. " + getAsNumber(selectedFiles.get(0).getName()) + " - " + getAsNumber(selectedFiles.get(selectedFiles.size() - 1).getName() + ")");

            DocumentFile file;

            if (type == 0) {
                file = PdfUtils.createPdfFromPngs(context, pngs, name);
            } else if (type == 1) {
                file = EpubUtils.generateEpub(context, pngs, name);
            } else {
                file = null;
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                manager.beginTransaction()
                        .replace(R.id.main, new FinishFragment(file))
                        .commit();
            });
        }).start();
    }

}