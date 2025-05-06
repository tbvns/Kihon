package xyz.tbvns.kihon;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import android.widget.Switch;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import xyz.tbvns.EZConfig;
import xyz.tbvns.kihon.Config.MainConfig;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());

        Switch manualSelectionSwitch = findViewById(R.id.manualSelectionSwitch);
        manualSelectionSwitch.setOnClickListener(l -> {
            MainConfig.manualSelection = manualSelectionSwitch.isChecked();
            try {
                EZConfig.save();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        manualSelectionSwitch.setChecked(MainConfig.manualSelection);
        Switch reEncodeSwitch = findViewById(R.id.reEncodeSwitch);
        reEncodeSwitch.setOnClickListener(l -> {
            MainConfig.reEncodeByDefault = reEncodeSwitch.isChecked();
            try {
                EZConfig.save();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        reEncodeSwitch.setChecked(MainConfig.reEncodeByDefault);
    }
}