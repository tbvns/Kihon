package xyz.tbvns.kihon.activity;

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
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import xyz.tbvns.EZConfig;
import xyz.tbvns.kihon.logic.ProgressManager;
import xyz.tbvns.kihon.ExportSetting;
import xyz.tbvns.kihon.Constants;
import xyz.tbvns.kihon.Formats.EpubUtils;
import xyz.tbvns.kihon.Formats.ImageUtils;
import xyz.tbvns.kihon.Formats.PdfUtils;
import xyz.tbvns.kihon.R;
import xyz.tbvns.kihon.Settings;
import xyz.tbvns.kihon.logic.Object.BrowserItem;
import xyz.tbvns.kihon.logic.Object.ChapterObject;
import xyz.tbvns.kihon.logic.Sorter;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ExportOptionsActivity extends AppCompatActivity {

    private ArrayList<ChapterObject> selectedFiles;
    private boolean reEncode;

    private Bitmap coverPageBitmap;
    private Bitmap coverCustomBitmap;
    private String selectedCoverFileName;
    private int selectedCoverPageIndex = 0;

    private ActivityResultLauncher<String> selectImageLauncher;

    private ImageView coverPreview;
    private TextView coverFileName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_options);

        selectedFiles = ExportOptionsLauncher.pendingFiles;
        reEncode = ExportOptionsLauncher.pendingReEncode;

        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        loadCoverImageFromUri(uri);
                    }
                }
        );

        setupUI();
    }

    @SneakyThrows
    private void setupUI() {
        EZConfig.load();

        Switch reencodeSwitch = findViewById(R.id.reencodeSwitch);
        View reEncodeOptions = findViewById(R.id.reEncodeOptions);
        SeekBar qualitySeekBar = findViewById(R.id.qualitySeekbar);
        TextView qualityText = findViewById(R.id.qualityText);

        Switch resizeSwitch = findViewById(R.id.reziseImageSwitch);
        View resizeOptions = findViewById(R.id.resizeOptions);
        SeekBar sizeSeekBar = findViewById(R.id.sizeSeekBar);
        TextView sizeText = findViewById(R.id.sizeText);

        Switch grayscaleSwitch = findViewById(R.id.grayscaleSwitch);

        Switch coverSwitch = findViewById(R.id.coverSwitch);
        View coverOptions = findViewById(R.id.coverOptions);

        Switch coverPageSwitch = findViewById(R.id.coverPageSwitch);
        View coverPageOptions = findViewById(R.id.coverPageOptions);
        EditText coverPageInput = findViewById(R.id.coverPageInput);

        Switch coverCustomSwitch = findViewById(R.id.CoverCustomSwitch);
        View coverCustomOptions = findViewById(R.id.coverCustomOptions);
        Button selectCoverFileBtn = findViewById(R.id.selectCoverFileBtn);

        coverPreview = findViewById(R.id.coverPreview);
        coverFileName = findViewById(R.id.coverFileName);

        String[] formats = {"Electronic Publication (ePUB)",
                "Portable Document Format (PDF)"};
        Spinner formatSpinner = findViewById(R.id.formatSpinner);
        ArrayAdapter<Object> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, formats);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        formatSpinner.setAdapter(adapter);

        findViewById(R.id.Proceed).setOnClickListener(c -> onProceedClicked(formatSpinner));
        findViewById(R.id.Cancel).setOnClickListener(v -> finish());

        CompoundButton.OnCheckedChangeListener imageProcessListener =
                (buttonView, isChecked) ->
                        updateReencodeStatus(reencodeSwitch, resizeSwitch, grayscaleSwitch);

        reencodeSwitch.setChecked(ExportSetting.REENCODE_IMAGES);
        reencodeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.REENCODE_IMAGES = isChecked;
            try { EZConfig.save(); } catch (Exception e) { throw new RuntimeException(e); }
            reEncodeOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            imageProcessListener.onCheckedChanged(buttonView, isChecked);
        });

        qualitySeekBar.setProgress(ExportSetting.IMAGE_QUALITY);
        qualitySeekBar.setOnSeekBarChangeListener(createSeekBarListener(progress -> {
            ExportSetting.IMAGE_QUALITY = progress;
            try { EZConfig.save(); } catch (Exception e) { throw new RuntimeException(e); }
            qualityText.setText(progress + "%");
        }));

        resizeSwitch.setChecked(ExportSetting.RESIZE_IMAGES);
        resizeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.RESIZE_IMAGES = isChecked;
            try { EZConfig.save(); } catch (Exception e) { throw new RuntimeException(e); }
            resizeOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            imageProcessListener.onCheckedChanged(buttonView, isChecked);
        });

        sizeSeekBar.setProgress(ExportSetting.IMAGE_SIZE);
        sizeSeekBar.setOnSeekBarChangeListener(createSeekBarListener(progress -> {
            ExportSetting.IMAGE_SIZE = progress;
            try { EZConfig.save(); } catch (Exception e) { throw new RuntimeException(e); }
            sizeText.setText(progress + "%");
        }));

        grayscaleSwitch.setChecked(ExportSetting.GRAYSCALE);
        grayscaleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.GRAYSCALE = isChecked;
            try { EZConfig.save(); } catch (Exception e) { throw new RuntimeException(e); }
            imageProcessListener.onCheckedChanged(buttonView, isChecked);
        });

        coverSwitch.setChecked(ExportSetting.USE_COVER);
        coverSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.USE_COVER = isChecked;
            try { EZConfig.save(); } catch (Exception e) { throw new RuntimeException(e); }
            coverOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        coverPageSwitch.setChecked(ExportSetting.COVER_USE_PAGE);
        coverPageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.COVER_USE_PAGE = isChecked;
            try { EZConfig.save(); } catch (Exception e) { throw new RuntimeException(e); }
            coverPageOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                coverCustomSwitch.setChecked(true);
            } else {
                coverCustomSwitch.setChecked(false);
            }
        });

        coverPageInput.setText(String.valueOf(ExportSetting.COVER_PAGE_INDEX + 1));
        coverPageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int pageNum = Integer.parseInt(coverPageInput.getText().toString().trim());
                    if (pageNum > 0 && pageNum <= selectedFiles.size()) {
                        selectedCoverPageIndex = pageNum - 1;
                        ExportSetting.COVER_PAGE_INDEX = pageNum - 1;
                        EZConfig.save();
                    } else {
                        Toast.makeText(this,
                                "Page number must be between 1 and " + selectedFiles.size(),
                                Toast.LENGTH_SHORT).show();
                        coverPageInput.setText(String.valueOf(ExportSetting.COVER_PAGE_INDEX + 1));
                        selectedCoverPageIndex = ExportSetting.COVER_PAGE_INDEX;
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    coverPageInput.setText(String.valueOf(ExportSetting.COVER_PAGE_INDEX + 1));
                    selectedCoverPageIndex = ExportSetting.COVER_PAGE_INDEX;
                }
            }
        });

        coverCustomSwitch.setChecked(ExportSetting.COVER_USE_CUSTOM);
        coverCustomSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ExportSetting.COVER_USE_CUSTOM = isChecked;
            try { EZConfig.save(); } catch (Exception e) { throw new RuntimeException(e); }
            coverCustomOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                coverPageSwitch.setChecked(true);
            } else {
                coverPageSwitch.setChecked(false);
            }
        });

        selectCoverFileBtn.setOnClickListener(v -> {
            Log.d("ExportOptions", "Select cover button clicked");
            selectImageLauncher.launch("image/*");
        });

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
    }

    private void onProceedClicked(Spinner formatSpinner) {
        selectedFiles.sort(Comparator.comparingInt(c -> c.number));
        if (formatSpinner.getSelectedItem().equals("Electronic Publication (ePUB)")) {
            generate(1, reEncode);
        } else {
            generate(0, reEncode);
        }
    }

    private SeekBar.OnSeekBarChangeListener createSeekBarListener(
            java.util.function.Consumer<Integer> onProgress) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onProgress.accept(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
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

    private void loadCoverImageFromUri(Uri uri) {
        try {
            Log.d("ExportOptions", "Loading cover image from URI: " + uri);
            InputStream inputStream = getContentResolver().openInputStream(uri);

            if (inputStream == null) {
                Log.e("ExportOptions", "InputStream is null");
                Toast.makeText(this, "Failed to open image", Toast.LENGTH_SHORT).show();
                return;
            }

            coverCustomBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (coverCustomBitmap == null) {
                Log.e("ExportOptions", "BitmapFactory returned null");
                Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("ExportOptions", "Image loaded successfully: " +
                    coverCustomBitmap.getWidth() + "x" + coverCustomBitmap.getHeight());

            // Save bitmap and store path
            String filePath = saveBitmapToFile(this, coverCustomBitmap);
            ExportSetting.COVER_CUSTOM_BITMAP_PATH = filePath;
            EZConfig.save();

            coverPreview.setImageBitmap(coverCustomBitmap);
            coverPreview.setVisibility(View.VISIBLE);

            String fileName = getFileNameFromUri(uri);
            coverFileName.setText("Loaded: " + fileName);
            selectedCoverFileName = fileName;

            Log.d("ExportOptions", "UI updated with cover image");
            Toast.makeText(this, "Cover image loaded successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("ExportOptions", "Error loading cover image", e);
            Toast.makeText(this, "Failed to load cover image: " + e.getMessage(),
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
                    coverFileName.setText("Loaded: cover.jpg");
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
        File file = new File(cacheDir, "cover.jpg");
        FileOutputStream fos = new FileOutputStream(file);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        fos.close();
        return file.getAbsolutePath();
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    @SneakyThrows
    private DocumentFile extractZip(Context context, DocumentFile zipFile) {
        try {
            if (Constants.ExtractedFile == null) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context,
                                "Extracted files directory not initialized!",
                                Toast.LENGTH_SHORT).show());
                return null;
            }

            File tempZipFile = new File(context.getCacheDir(), zipFile.getName());
            InputStream inputStream = context.getContentResolver()
                    .openInputStream(zipFile.getUri());
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
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Failed to create extraction folder!",
                                Toast.LENGTH_SHORT).show());
                return null;
            }

            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    extractFolder.createDirectory(entry.getName());
                } else {
                    String name = entry.getName().toLowerCase();
                    if (name.endsWith(".png") || name.endsWith(".jpg") ||
                            name.endsWith(".jpeg") || name.endsWith(".webp")) {

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

            return extractFolder;

        } catch (IOException e) {
            Log.e("ExportOptions", "Error extracting ZIP file", e);
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "Failed to extract ZIP file!",
                            Toast.LENGTH_SHORT).show());
        }
        return null;
    }

    public static int getAsNumber(String str) {
        if (str == null) return -1;
        return Integer.parseInt(str.replaceAll("\\D", "").replace(".", "").strip());
    }

    @AllArgsConstructor
    public static class warningDialog extends DialogFragment {
        private DialogInterface.OnClickListener first;
        private DialogInterface.OnClickListener second;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Using ePUB files with a large number of chapters may lead to " +
                            "crashes due to excessive memory consumption. To avoid this issue, " +
                            "consider using PDF format instead.")
                    .setTitle("Warning")
                    .setNegativeButton("Continue", first)
                    .setPositiveButton("Switch to PDF", second);
            return builder.create();
        }
    }

    public void generate(int type, boolean reencode) {
        Context context = this;
        FragmentManager manager = getSupportFragmentManager();
        ProgressManager pm = ProgressManager.getInstance();

        new Thread(() -> {
            String formatName = (type == 0) ? "PDF" : "EPUB";
            pm.startProgress(context, "Exporting " + formatName, "Extracting archives...");
            pm.setItemsCount(0, selectedFiles.size());

            List<DocumentFile> pngs = new ArrayList<>();
            List<Integer> chapterBoundaries = new ArrayList<>();  // NEW: Track chapter starts

            int max = selectedFiles.size();
            List<DocumentFile> files = selectedFiles.stream()
                    .map(i -> i.file)
                    .collect(Collectors.toCollection(ArrayList::new));

            for (int i = 0; i < files.size(); i++) {
                DocumentFile file = files.get(i);
                pm.updateMessage("Extracting: " + file.getName());
                pm.setCurrentTask("Archive " + (i + 1) + " of " + max);
                pm.setItemsCount(i + 1, max);

                chapterBoundaries.add(pngs.size());

                DocumentFile e = extractZip(context, file);
                if (e != null) {
                    DocumentFile[] listFiles = e.listFiles();

                    List<DocumentFile> chapterFiles = new ArrayList<>();
                    if (listFiles != null) {
                        for (DocumentFile listFile : listFiles) {
                            if (listFile.isFile()) {
                                chapterFiles.add(listFile);
                            }
                        }
                    }

                    chapterFiles.sort((f1, f2) -> {
                        String name1 = f1.getName();
                        String name2 = f2.getName();

                        if (name1 == null || name2 == null) return 0;

                        Sorter.ParsedFileName p1 = Sorter.parseFileName(name1);
                        Sorter.ParsedFileName p2 = Sorter.parseFileName(name2);

                        if (p1 == null || p2 == null) return name1.compareTo(name2);

                        if (p1.major != p2.major) {
                            return Integer.compare(p1.major, p2.major);
                        }
                        return Integer.compare(p1.minor, p2.minor);
                    });

                    pngs.addAll(chapterFiles);
                }

                int extractProgress = (int) ((float) (i + 1) / max * 40);
                pm.updateProgress(extractProgress);
            }

            pm.updateProgress(40);
            pm.updateMessage("Generating file name...");
            pm.setCurrentTask("Preparing metadata");

            String name = new ArrayList<>(selectedFiles).get(0).file.getParentFile().getName()
                    + " from "
                    + new ArrayList<>(selectedFiles).get(0).title
                    + " to "
                    + new ArrayList<>(selectedFiles).get(selectedFiles.size() - 1).title;

            DocumentFile file;

            if (reencode) {
                pm.updateProgress(40);
                pm.updateMessage("Processing images...");
                pm.setCurrentTask("Optimizing images");
                try {
                    ImageUtils.processImagesNoInit(context, pngs);
                    pm.updateProgress(60);
                } catch (IOException e) {
                    pm.updateMessage("Error processing images: " + e.getMessage());
                    pm.setCurrentTask("Failed");
                    try { Thread.sleep(2000); } catch (InterruptedException ex) {}
                    pm.finishProgress();
                    return;
                }
            } else {
                pm.updateProgress(60);
            }

            pm.updateProgress(60);
            if (type == 0) {
                pm.updateMessage("Generating PDF...");
                pm.setCurrentTask("Adding images to PDF");
                file = PdfUtils.createPdfFromPngsNoInit(context, pngs, name, chapterBoundaries);
            } else if (type == 1) {
                pm.updateMessage("Generating EPUB...");
                pm.setCurrentTask("Creating EPUB structure");
                file = EpubUtils.generateEpubNoInit(context, pngs, name, chapterBoundaries);
            } else {
                file = null;
            }

            if (file != null) {
                pm.updateProgress(100);
                pm.updateMessage("Export completed successfully!");
                pm.setCurrentTask("Complete");
                try { Thread.sleep(1500); } catch (InterruptedException ex) {}
            } else {
                pm.updateMessage("Export failed!");
                pm.setCurrentTask("Failed");
                try { Thread.sleep(2000); } catch (InterruptedException ex) {}
            }
            pm.finishProgress();

            final DocumentFile finalFile = file;
            new Handler(Looper.getMainLooper()).post(() -> {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (type == 0) {
                        try {
                            manager.popBackStack("export", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                            manager.beginTransaction()
                                    .setReorderingAllowed(true)
                                    .addToBackStack("export")
                                    .commitAllowingStateLoss();
                        } catch (IllegalStateException e) {
                            Log.e("ExportOptions", "Fragment transaction failed", e);
                        }
                    } else if (type == 1) {
                        try {
                            manager.popBackStack("export", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                            manager.beginTransaction()
                                    .setReorderingAllowed(true)
                                    .addToBackStack("export")
                                    .commitAllowingStateLoss();
                        } catch (IllegalStateException e) {
                            Log.e("ExportOptions", "Fragment transaction failed", e);
                        }
                    }
                }, 1500);
            });
        }).start();
    }

    private void resetConstants() {
        ExportSetting.REENCODE_IMAGES = Settings.reEncodeByDefault;
        ExportSetting.IMAGE_QUALITY = 100;
        ExportSetting.RESIZE_IMAGES = false;
        ExportSetting.IMAGE_SIZE = 100;
        ExportSetting.GRAYSCALE = false;
        ExportSetting.USE_COVER = true;
        ExportSetting.COVER_USE_PAGE = true;
        ExportSetting.COVER_USE_CUSTOM = false;
        ExportSetting.COVER_PAGE_INDEX = 0;
        ExportSetting.secondaryActionImpact = Settings.reEncodeByDefault ? 0.25F : 0.5F;
    }

    public static class ExportOptionsLauncher {
        public static ArrayList<ChapterObject> pendingFiles;
        public static boolean pendingReEncode;
    }
}