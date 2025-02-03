package xyz.tbvns.kihon;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;

public class ShareUtils {

    /**
     * Opens the Android share menu to share a PDF file.
     *
     * @param context  the Context
     * @param pdfFile  the DocumentFile representing the PDF file
     */
    public static void sharePdfFile(Context context, DocumentFile pdfFile) {
        if (pdfFile == null || !pdfFile.exists() || !pdfFile.getName().endsWith(".pdf")) {
            return; // Exit if the file is null, does not exist, or is not a PDF
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");

        // Get the URI of the file
        Uri fileUri = pdfFile.getUri();
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Open the share menu
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF via"));
    }

    public static void openPdfFile(Context context, DocumentFile documentFile) {
        // Get the URI from the DocumentFile
        Uri pdfUri = documentFile.getUri();

        // Create an intent to view the PDF
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");

        // Grant temporary read permission to the target app
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Verify there's an app to handle the intent
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                showErrorToast(context, "Failed to open PDF file");
            }
        } else {
            showErrorToast(context, "No PDF viewer app installed");
        }
    }

    private static void showErrorToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
