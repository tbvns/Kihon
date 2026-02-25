package xyz.tbvns.kihon.Formats;

import android.content.Context;
import android.graphics.*;
import androidx.documentfile.provider.DocumentFile;
import xyz.tbvns.kihon.logic.ProgressManager;
import xyz.tbvns.kihon.ExportSetting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ImageUtils {

    public static byte[] reencodeToJpeg(InputStream inputStream, int quality) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

        ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, jpegStream);
        return jpegStream.toByteArray();
    }

    public static Bitmap resizeBitmap(Bitmap original, float scalePercent) {
        int width = (int) (original.getWidth() * scalePercent);
        int height = (int) (original.getHeight() * scalePercent);
        return Bitmap.createScaledBitmap(original, width, height, true);
    }

    public static Bitmap toGrayscale(Bitmap original) {
        Bitmap grayscale = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayscale);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        canvas.drawBitmap(original, 0, 0, paint);
        return grayscale;
    }

    /**
     * Process images with optional grayscale, resizing, and re-encoding.
     * This version STARTS its own progress activity.
     * Use this when calling ImageUtils standalone.
     */
    public static void processImages(Context context, List<DocumentFile> pngFiles) throws IOException {
        ProgressManager pm = ProgressManager.getInstance();
        pm.startProgress(context, "Processing Images", "Optimizing images...");
        processImagesInternal(context, pngFiles, true);
        pm.finishProgress();
    }

    /**
     * Process images WITHOUT starting/finishing progress activity.
     * Use this when called from within an existing export flow (ExportOptionsActivity).
     * Progress should be managed by the caller (40-60% range).
     */
    public static void processImagesNoInit(Context context, List<DocumentFile> pngFiles) throws IOException {
        processImagesInternal(context, pngFiles, false);
    }

    /**
     * Internal image processing implementation.
     * @param initProgress true if this started the progress (show completion), false otherwise
     */
    private static void processImagesInternal(Context context, List<DocumentFile> pngFiles, boolean initProgress) throws IOException {
        ProgressManager pm = ProgressManager.getInstance();
        int total = pngFiles.size();

        if (initProgress) {
            pm.setItemsCount(0, total);
        }

        int imageIndex = 0;
        for (DocumentFile file : pngFiles) {
            imageIndex++;

            if (!file.isFile() || !file.getName().toLowerCase().matches(".*\\.(png|jpg|jpeg)$")) {
                continue;
            }

            String fileName = file.getName();
            pm.updateMessage("Processing: " + fileName);
            pm.setCurrentTask("Image " + imageIndex + " of " + total);
            pm.setItemsCount(imageIndex, total);

            try (InputStream inputStream = context.getContentResolver().openInputStream(file.getUri())) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (bitmap == null) {
                    pm.updateMessage("Skipped: " + fileName + " (invalid image)");
                    if (initProgress) {
                        pm.updateProgress((imageIndex * 100) / total);
                    } else {
                        pm.updateProgress(40 + (int) ((float) imageIndex / total * 20));
                    }
                    continue;
                }

                if (ExportSetting.GRAYSCALE) {
                    pm.setCurrentTask("Converting to grayscale");
                    bitmap = toGrayscale(bitmap);
                }

                if (ExportSetting.RESIZE_IMAGES) {
                    pm.setCurrentTask("Resizing image");
                    float scale = ExportSetting.IMAGE_SIZE / 100f;
                    bitmap = resizeBitmap(bitmap, scale);
                }

                try (OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri())) {
                    if (outputStream == null) {
                        pm.updateMessage("Skipped: " + fileName + " (cannot write)");
                        if (initProgress) {
                            pm.updateProgress((imageIndex * 100) / total);
                        } else {
                            pm.updateProgress(40 + (int) ((float) imageIndex / total * 20));
                        }
                        continue;
                    }

                    pm.setCurrentTask("Saving image");
                    if (ExportSetting.REENCODE_IMAGES) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, ExportSetting.IMAGE_QUALITY, outputStream);
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    }
                }

                bitmap.recycle();
                pm.updateMessage("Processed: " + fileName);

                if (initProgress) {
                    pm.updateProgress((imageIndex * 100) / total);
                } else {
                    pm.updateProgress(40 + (int) ((float) imageIndex / total * 20));
                }

            } catch (Exception e) {
                pm.updateMessage("Error processing: " + fileName);
                e.printStackTrace();
            }
        }

        if (initProgress) {
            pm.updateProgress(100);
            pm.updateMessage("Image processing complete!");
            pm.setCurrentTask("Complete");
        }
    }
}