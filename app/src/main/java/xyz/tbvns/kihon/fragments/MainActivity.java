package xyz.tbvns.kihon.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import xyz.tbvns.kihon.Constant;
import xyz.tbvns.kihon.R;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_FOLDER = 123; // Define your request code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Button button = findViewById(R.id.button);
        button.setOnClickListener(a -> {
            System.out.println("Button !");
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CODE_FOLDER);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            // Calculate the permission flags from the returned Intent
            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                // You can now use treeUri to access files within the selected folder.
                Log.d("MainActivity", "Persisted URI permission for: " + treeUri.toString());

                // List files using DocumentFile API
                listFilesInFolder(treeUri);
            } catch (SecurityException e) {
                Log.e("MainActivity", "Failed to persist URI permission: " + e.getMessage());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void listFilesInFolder(Uri folderUri) {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, folderUri);
        boolean createExtract = true;
        for (DocumentFile file : pickedDir.listFiles()) {
             if (file.getName().equals("extracted")) {
                Constant.ExtractedFile = file;
                createExtract = false;
            }
        }
        if (createExtract) {
            Constant.ExtractedFile = pickedDir.createDirectory("extracted");
        }
        for (DocumentFile file : pickedDir.listFiles()) {
            if (file.getName().equals("downloads")) {
                pickedDir = file;
            }
        }

        for (DocumentFile file : pickedDir.listFiles()) {
            System.out.println(file.getName());
        }
        List<DocumentFile> files = Arrays.asList(pickedDir.listFiles());

        // Replace activity content with BlankFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, new FileFragment(files))
                .commit();
    }
}
