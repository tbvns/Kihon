package xyz.tbvns.kihon;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import com.google.android.material.appbar.MaterialToolbar;
import lombok.SneakyThrows;
import xyz.tbvns.EZConfig;
import xyz.tbvns.kihon.Config.MainConfig;
import xyz.tbvns.kihon.fragments.ExportOptions;
import xyz.tbvns.kihon.fragments.FileFragment;
import xyz.tbvns.kihon.fragments.StartFragment;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_FOLDER = 123; // Define your request code

    @SneakyThrows //Yea I know this shit is bad but idc it looks better !
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EZConfig.setConfigFolder(getFilesDir().getPath());
        EZConfig.getRegisteredClasses().add(MainConfig.class);
        EZConfig.load();
        EZConfig.save();

        ImageButton topAppBar = findViewById(R.id.settingIcon);
        topAppBar.setOnClickListener(view -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, new StartFragment())
                .disallowAddToBackStack()
                .commit();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Button button = findViewById(R.id.selectMihonFolder);
        button.setOnClickListener(a -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CODE_FOLDER);
        });
        button = findViewById(R.id.back);
        button.setOnClickListener(a -> {
            getSupportFragmentManager().popBackStack();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                Log.d("MainActivity", "Persisted URI permission for: " + treeUri.toString());
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
        List<DocumentFile> files = Arrays.asList(pickedDir.listFiles());

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, new FileFragment(files))
                .addToBackStack("files")
                .commit();
    }
}
