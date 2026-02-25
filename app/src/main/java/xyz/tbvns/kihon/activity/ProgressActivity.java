package xyz.tbvns.kihon.activity;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import xyz.tbvns.kihon.R;
import xyz.tbvns.kihon.logic.ProgressManager;


public class ProgressActivity extends AppCompatActivity {

    private ProgressBar progressSpinner;
    private ProgressBar progressBar;
    private TextView progressTitle;
    private TextView progressMessage;
    private TextView progressPercentage;
    private TextView progressCurrentTask;
    private TextView progressItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        progressSpinner = findViewById(R.id.progress_spinner);
        progressBar = findViewById(R.id.progress_bar);
        progressTitle = findViewById(R.id.progress_title);
        progressMessage = findViewById(R.id.progress_message);
        progressPercentage = findViewById(R.id.progress_percentage);
        progressCurrentTask = findViewById(R.id.progress_current_task);
        progressItems = findViewById(R.id.progress_items);

        setFinishOnTouchOutside(false);

        ProgressManager.getInstance().registerActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProgressManager.getInstance().unregisterActivity();
    }

    @Override
    public void onBackPressed() {
        if (!ProgressManager.getInstance().isFinished()) {
            return;
        }
        super.onBackPressed();
    }

    public void setProgressBar(int progress) {
        progress = Math.max(0, Math.min(100, progress));
        progressBar.setProgress(progress);
        progressPercentage.setText(progress + "%");
    }

    /**
     * Update the title message
     */
    public void setTitle(String title) {
        progressTitle.setText(title);
    }

    public void setMessage(String message) {
        progressMessage.setText(message);
    }

    public void setCurrentTask(String task) {
        progressCurrentTask.setText(task);
    }

    public void setItemsCount(int current, int total) {
        progressItems.setText(current + "/" + total);
    }

    public void reset() {
        progressBar.setProgress(0);
        progressPercentage.setText("0%");
        progressTitle.setText("Processing...");
        progressMessage.setText("Initializing...");
        progressCurrentTask.setText("Initializing...");
        progressItems.setText("0/0");
    }
}