package xyz.tbvns.kihon.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import xyz.tbvns.EZConfig;
import xyz.tbvns.kihon.Config.MainConfig;
import xyz.tbvns.kihon.Config.ExportSetting;
import xyz.tbvns.kihon.Constants;
import xyz.tbvns.kihon.Formats.EpubUtils;
import xyz.tbvns.kihon.Formats.ImageUtils;
import xyz.tbvns.kihon.Formats.PdfUtils;
import xyz.tbvns.kihon.R;

import java.io.*;
import java.util.*;

@AllArgsConstructor
public class ExportOptions extends Fragment {
    private List<DocumentFile> selectedFiles;
    private boolean reEncode;

    public ExportOptions(List<DocumentFile> selectedFiles, boolean reEncode) {
        this.selectedFiles = selectedFiles;
        this.reEncode = reEncode;
    }

    private Bitmap coverPageBitmap;
    private Bitmap coverCustomBitmap;
    private String selectedCoverFileName;
    private int selectedCoverPageIndex = 0;

    private ActivityResultContracts.GetContent getContentLauncher;

    private ActivityResultLauncher<String> selectImageLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the activity result launcher for file selection
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        loadCoverImageFromUri(uri);
                    }
                }
        );
    }

    private void onProceedClicked(Spinner formatSpinner) {
        if (formatSpinner.getSelectedItem()
                .equals("Electronic Publication (ePUB)")) {
            if (selectedFiles.size() >= 5) {
                new warningDialog(
                        (dialog, which) -> generate(1, reEncode),
                        (dialog, which) -> generate(0, reEncode)
                ).show(getParentFragmentManager(), "warningMemory");
            } else {
                generate(1, reEncode);
            }
        } else {
            generate(0, reEncode);
        }
    }

    private SeekBar.OnSeekBarChangeListener createSeekBarListener(
            java.util.function.Consumer<Integer> onProgress) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                onProgress.accept(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

    private void updateReencodeStatus(Switch... switches) {
        for (Switch s : switches) {
            if (s.isChecked()) {
                ExportSetting.secondaryActionImpact = 0.25F;
                reEncode = true;
                return;
            }
        }
        ExportSetting.secondaryActionImpact = 0.5F;
        reEncode = false;
    }

    @SneakyThrows
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_export_options, container, false);

        // Load saved preferences
        EZConfig.load();

        Context context = requireContext();

        // Image processing references
        Switch reencodeSwitch = view.findViewById(R.id.reencodeSwitch);
        View reEncodeOptions = view.findViewById(R.id.reEncodeOptions);
        SeekBar qualitySeekBar = view.findViewById(R.id.qualitySeekbar);
        TextView qualityText = view.findViewById(R.id.qualityText);

        Switch resizeSwitch = view.findViewById(R.id.reziseImageSwitch);
        View resizeOptions = view.findViewById(R.id.resizeOptions);
        SeekBar sizeSeekBar = view.findViewById(R.id.sizeSeekBar);
        TextView sizeText = view.findViewById(R.id.sizeText);

        Switch grayscaleSwitch = view.findViewById(R.id.grayscaleSwitch);

        // Cover references
        Switch coverSwitch = view.findViewById(R.id.coverSwitch);
        View coverOptions = view.findViewById(R.id.coverOptions);

        Switch coverPageSwitch = view.findViewById(R.id.coverPageSwitch);
        View coverPageOptions = view.findViewById(R.id.coverPageOptions);
        EditText coverPageInput = view.findViewById(R.id.coverPageInput);

        Switch coverCustomSwitch = view.findViewById(R.id.CoverCustomSwitch);
        View coverCustomOptions = view.findViewById(R.id.coverCustomOptions);
        Button selectCoverFileBtn = view.findViewById(R.id.selectCoverFileBtn);
        ImageView coverPreview = view.findViewById(R.id.coverPreview);
        TextView coverFileName = view.findViewById(R.id.coverFileName);

        // Setup format spinner
        String[] formats = {"Electronic Publication (ePUB)",
                "Portable Document Format (PDF)"};
        Spinner formatSpinner = view.findViewById(R.id.formatSpinner);
        ArrayAdapter<Object> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, formats);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        formatSpinner.setAdapter(adapter);

        // Setup buttons
        view.findViewById(R.id.Proceed).setOnClickListener(
                c -> onProceedClicked(formatSpinner));
        view.findViewById(R.id.Cancel).setOnClickListener(
                v -> getParentFragmentManager().popBackStack());

        // Common switch listener
        CompoundButton.OnCheckedChangeListener imageProcessListener =
                (buttonView, isChecked) ->
                        updateReencodeStatus(reencodeSwitch, resizeSwitch,
                                grayscaleSwitch);

        // Re-encode switch and quality slider
        reencodeSwitch.setChecked(ExportSetting.REENCODE_IMAGES);
        reencodeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.REENCODE_IMAGES = isChecked;
            try {
                EZConfig.save();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            reEncodeOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            imageProcessListener.onCheckedChanged(buttonView, isChecked);
        });

        qualitySeekBar.setProgress(ExportSetting.IMAGE_QUALITY);
        qualitySeekBar.setOnSeekBarChangeListener(createSeekBarListener(
                progress -> {
                    ExportSetting.IMAGE_QUALITY = progress;
                    try {
                        EZConfig.save();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    qualityText.setText(progress + "%");
                }
        ));

        // Resize switch and size slider
        resizeSwitch.setChecked(ExportSetting.RESIZE_IMAGES);
        resizeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.RESIZE_IMAGES = isChecked;
            try {
                EZConfig.save();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            resizeOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            imageProcessListener.onCheckedChanged(buttonView, isChecked);
        });

        sizeSeekBar.setProgress(ExportSetting.IMAGE_SIZE);
        sizeSeekBar.setOnSeekBarChangeListener(createSeekBarListener(
                progress -> {
                    ExportSetting.IMAGE_SIZE = progress;
                    try {
                        EZConfig.save();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    sizeText.setText(progress + "%");
                }
        ));

        // Grayscale switch
        grayscaleSwitch.setChecked(ExportSetting.GRAYSCALE);
        grayscaleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.GRAYSCALE = isChecked;
            try {
                EZConfig.save();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            imageProcessListener.onCheckedChanged(buttonView, isChecked);
        });

        // Main cover switch
        coverSwitch.setChecked(ExportSetting.USE_COVER);
        coverSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.USE_COVER = isChecked;
            try {
                EZConfig.save();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            coverOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Cover page switch
        coverPageSwitch.setChecked(ExportSetting.COVER_USE_PAGE);
        coverPageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.COVER_USE_PAGE = isChecked;
            try {
                EZConfig.save();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            coverPageOptions.setVisibility(
                    isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                coverCustomSwitch.setChecked(true);
            } else {
                coverCustomSwitch.setChecked(false);
            }
        });

        // Cover page input
        coverPageInput.setText(String.valueOf(ExportSetting.COVER_PAGE_INDEX + 1));
        coverPageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int pageNum = Integer.parseInt(
                            coverPageInput.getText().toString().trim());
                    if (pageNum > 0 && pageNum <= selectedFiles.size()) {
                        selectedCoverPageIndex = pageNum - 1;
                        ExportSetting.COVER_PAGE_INDEX = pageNum - 1;
                        EZConfig.save();
                    } else {
                        Toast.makeText(requireContext(),
                                "Page number must be between 1 and " +
                                        selectedFiles.size(),
                                Toast.LENGTH_SHORT).show();
                        coverPageInput.setText(String.valueOf(
                                ExportSetting.COVER_PAGE_INDEX + 1));
                        selectedCoverPageIndex = ExportSetting.COVER_PAGE_INDEX;
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(),
                            "Please enter a valid number",
                            Toast.LENGTH_SHORT).show();
                    coverPageInput.setText(String.valueOf(
                            ExportSetting.COVER_PAGE_INDEX + 1));
                    selectedCoverPageIndex = ExportSetting.COVER_PAGE_INDEX;
                }
            }
        });

        // Cover custom file switch
        coverCustomSwitch.setChecked(ExportSetting.COVER_USE_CUSTOM);
        coverCustomSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.COVER_USE_CUSTOM = isChecked;
            try {
                EZConfig.save();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            coverCustomOptions.setVisibility(
                    isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                coverPageSwitch.setChecked(true);
            } else {
                coverPageSwitch.setChecked(false);
            }
        });

        // File selector button
        selectCoverFileBtn.setOnClickListener(v -> {
            Log.d("ExportOptions", "Select cover button clicked");
            selectImageLauncher.launch("image/*");
        });

        // Load and display saved cover image if it exists
        if (!ExportSetting.COVER_CUSTOM_BITMAP_PATH.isEmpty()) {
            loadCoverImageFromPath(ExportSetting.COVER_CUSTOM_BITMAP_PATH,
                    coverPreview, coverFileName);
        }

        // Update UI visibility based on current state
        reEncodeOptions.setVisibility(ExportSetting.REENCODE_IMAGES ? View.VISIBLE : View.GONE);
        resizeOptions.setVisibility(ExportSetting.RESIZE_IMAGES ? View.VISIBLE : View.GONE);
        coverOptions.setVisibility(ExportSetting.USE_COVER ? View.VISIBLE : View.GONE);
        coverPageOptions.setVisibility(ExportSetting.COVER_USE_PAGE ? View.VISIBLE : View.GONE);
        coverCustomOptions.setVisibility(ExportSetting.COVER_USE_CUSTOM ? View.VISIBLE : View.GONE);

        updateReencodeStatus(reencodeSwitch, resizeSwitch, grayscaleSwitch);

        return view;
    }

    private void setupCoverPageSpinner(Spinner spinner) {
        List<String> pageNumbers = new ArrayList<>();
        if (selectedFiles != null) {
            for (int i = 0; i < selectedFiles.size(); i++) {
                pageNumbers.add("Page " + (i + 1));
            }
        }

        if (pageNumbers.isEmpty()) {
            pageNumbers.add("No pages available");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, pageNumbers);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void openFilePicker() {
        selectImageLauncher.launch("image/*");
    }


    private void loadCoverImageFromUri(Uri uri) {
        try {
            Log.d("ExportOptions", "Loading cover image from URI: " + uri);
            Context context = requireContext();
            InputStream inputStream =
                    context.getContentResolver().openInputStream(uri);

            if (inputStream == null) {
                Log.e("ExportOptions", "InputStream is null");
                Toast.makeText(context,
                        "Failed to open image",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            coverCustomBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (coverCustomBitmap == null) {
                Log.e("ExportOptions", "BitmapFactory returned null");
                Toast.makeText(context,
                        "Failed to decode image",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("ExportOptions", "Image loaded successfully: " +
                    coverCustomBitmap.getWidth() + "x" +
                    coverCustomBitmap.getHeight());

            View view = getView();
            if (view != null) {
                ImageView coverPreview = view.findViewById(R.id.coverPreview);
                TextView coverFileName = view.findViewById(R.id.coverFileName);

                // Save bitmap and store path
                String filePath = saveBitmapToFile(context, coverCustomBitmap);
                ExportSetting.COVER_CUSTOM_BITMAP_PATH = filePath;
                EZConfig.save();

                coverPreview.setImageBitmap(coverCustomBitmap);
                coverPreview.setVisibility(View.VISIBLE);

                String fileName = getFileNameFromUri(uri);
                coverFileName.setText("Loaded: " + fileName);
                selectedCoverFileName = fileName;

                Log.d("ExportOptions", "UI updated with cover image");
            }

            Toast.makeText(context, "Cover image loaded successfully",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("ExportOptions", "Error loading cover image", e);
            Toast.makeText(requireContext(),
                    "Failed to load cover image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCoverImageFromPath(String filePath, ImageView coverPreview,
                                        TextView coverFileName) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                if (bitmap != null) {
                    coverCustomBitmap = bitmap;
                    coverPreview.setImageBitmap(bitmap);
                    coverPreview.setVisibility(View.VISIBLE);
                    coverFileName.setText("Loaded: cover.png");
                    Log.d("ExportOptions", "Loaded saved cover image from: " + filePath);
                }
            }
        } catch (Exception e) {
            Log.e("ExportOptions", "Error loading saved cover image", e);
        }
    }

    private String saveBitmapToFile(Context context, Bitmap bitmap) throws IOException {
        File cacheDir = new File(context.getCacheDir(), "cover_images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        File file = new File(cacheDir, "cover.png");
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();

        return file.getAbsolutePath();
    }
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = requireContext().getContentResolver()
                    .query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @SneakyThrows
    private DocumentFile extractZip(Context context, DocumentFile zipFile) {
        try {
            // Check if ExtractedFile is null
            if (Constants.ExtractedFile == null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Extracted files directory not initialized!",
                            Toast.LENGTH_SHORT).show();
                });
                return null;
            }

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

            ZipFile zip = new ZipFile(tempZipFile);

            String zipFileName = zipFile.getName();
            if (zipFileName == null) return null;
            if (zipFileName.endsWith(".zip") || zipFileName.endsWith(".cbz")) {
                zipFileName = zipFileName.substring(0, zipFileName.length() - 4);
            }

            DocumentFile extractFolder = Constants.ExtractedFile.createDirectory(zipFileName);
            if (extractFolder == null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Failed to create extraction folder!",
                            Toast.LENGTH_SHORT).show();
                });
                return null;
            }

            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    extractFolder.createDirectory(entry.getName());
                } else {
                    if (entry.getName().endsWith(".png") || entry.getName().endsWith(".jpg") ||
                            entry.getName().endsWith(".jpeg")) {
                        DocumentFile newFile = extractFolder.createFile("image/*", entry.getName());
                        if (newFile != null) {
                            OutputStream outputStream = context.getContentResolver()
                                    .openOutputStream(newFile.getUri());
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
            tempZipFile.delete();

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "ZIP Extracted to " + extractFolder.getUri(),
                        Toast.LENGTH_SHORT).show();
            });
            return extractFolder;
        } catch (IOException e) {
            Log.e("ExportOptions", "Error extracting ZIP file", e);
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
                String[] split = file.getUri().getPath().toString().split("/");
                int id = Integer.parseInt(split[split.length-1].split("_")[0].replaceAll("\\D", "").replace(".", "").strip());
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

    public static HashSet<DocumentFile> sort(HashSet<DocumentFile> doc) {
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
        HashSet<DocumentFile> sortedFiles = new HashSet<>();
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

    public void generate(int type, boolean reencode) {
        Context context = requireContext();
        FragmentManager manager = getParentFragmentManager();

        new Thread(() -> {
            manager.beginTransaction()
                    .replace(R.id.main, new LoadingFragment())
                    .addToBackStack("export")
                    .commit();

            List<DocumentFile> pngs = new ArrayList<>();
            int max = selectedFiles.size();
            float percent = 0;
            List<DocumentFile> files = MainConfig.manualSelection ? selectedFiles : sort(selectedFiles);
            for (DocumentFile file : files) {
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

            String name = new ArrayList<>(selectedFiles).get(0).getParentFile().getName() + " Ch. " + getAsNumber(new ArrayList<>(selectedFiles).get(0).getName()) + " - " + getAsNumber(new ArrayList<>(selectedFiles).get(selectedFiles.size() - 1).getName() + ")");

            DocumentFile file;

            if (reencode) {
                try {
                    ImageUtils.processImages(context, pngs);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (type == 0) {
                file = PdfUtils.createPdfFromPngs(context, pngs, name);
            } else if (type == 1) {
                file = EpubUtils.generateEpub(context, pngs, name);
            } else {
                file = null;
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                if (type == 0) {
                    manager.popBackStack("export", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    manager.beginTransaction()
                            .replace(R.id.main, new FinishFragment(file, "application/pdf"))
                            .addToBackStack("export")
                            .commit();
                } else if (type == 1) {
                    manager.popBackStack("export", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    manager.beginTransaction()
                            .replace(R.id.main, new FinishFragment(file, "application/epub"))
                            .addToBackStack("export")
                            .commit();
                }
            });
        }).start();
    }

    private void resetConstants() {
        ExportSetting.REENCODE_IMAGES = MainConfig.reEncodeByDefault;
        ExportSetting.IMAGE_QUALITY = 100;
        ExportSetting.RESIZE_IMAGES = false;
        ExportSetting.IMAGE_SIZE = 100;
        ExportSetting.GRAYSCALE = false;
        ExportSetting.USE_COVER = true;
        ExportSetting.COVER_USE_PAGE = true;
        ExportSetting.COVER_USE_CUSTOM = false;
        ExportSetting.COVER_PAGE_INDEX = 0;
        ExportSetting.secondaryActionImpact = MainConfig.reEncodeByDefault ? 0.25F : 0.5F;
    }

}