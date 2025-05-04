package xyz.tbvns.kihon.Formats;

import android.content.Context;
import android.graphics.*;
import androidx.documentfile.provider.DocumentFile;
import xyz.tbvns.kihon.Constant;
import xyz.tbvns.kihon.fragments.FinishFragment;
import xyz.tbvns.kihon.fragments.LoadingFragment;

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

    public static void processImages(Context context, List<DocumentFile> pngFiles) throws IOException {
        for (DocumentFile file : pngFiles) {
            LoadingFragment.progress +=  ((float) 1 / pngFiles.size()) * 100 * Constant.secondaryActionImpact;
            LoadingFragment.message = "Processing image: " + file.getName();

            if (!file.isFile() || !file.getName().toLowerCase().matches(".*\\.(png|jpg|jpeg)$")) {
                continue;
            }

            try (InputStream inputStream = context.getContentResolver().openInputStream(file.getUri())) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (bitmap == null) continue;

                if (Constant.GRAYSCALE) {
                    bitmap = toGrayscale(bitmap);
                }

                if (Constant.RESIZE_IMAGES) {
                    float scale = Constant.IMAGE_SIZE / 100f;
                    bitmap = resizeBitmap(bitmap, scale);
                }

                try (OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri())) {
                    if (outputStream == null) continue;

                    if (Constant.REENCODE_IMAGES) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, Constant.IMAGE_QUALITY, outputStream);
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    }
                }
            }
        }
    }
}
