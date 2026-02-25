package xyz.tbvns.kihon;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.documentfile.provider.DocumentFile;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import xyz.tbvns.EZConfig;
import xyz.tbvns.kihon.databinding.ActivityMainBinding;
import xyz.tbvns.kihon.logic.FilesLogic;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    boolean hadOpened = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Load settings
        try {
            EZConfig.getRegisteredClasses().add(Settings.class);
            EZConfig.getRegisteredClasses().add(ExportSetting.class);
            EZConfig.setConfigFolder(getFilesDir().getPath());
            EZConfig.load();
            EZConfig.save();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configs: " + e.getMessage());
        }

        if (Settings.mihonPath != null) {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, Uri.parse(Settings.mihonPath));
            boolean createExtract = true;
            for (DocumentFile file : pickedDir.listFiles()) {
                if (file.getName().equals("extracted")) {
                    Constants.ExtractedFile = file;
                    createExtract = false;
                }
            }
            if (createExtract) {
                Constants.ExtractedFile = pickedDir.createDirectory("extracted");
            }
        }

        FilesLogic.init(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_convert, R.id.navigation_manage, R.id.navigation_settings)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        if (getIntent().getBooleanExtra("manage", false)) {
            binding.navView.setSelectedItemId(R.id.navigation_manage);
        }
    }

}