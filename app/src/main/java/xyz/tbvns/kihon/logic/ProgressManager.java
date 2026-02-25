package xyz.tbvns.kihon.logic;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.MainThread;
import xyz.tbvns.kihon.activity.ProgressActivity;

public class ProgressManager {
    private static ProgressManager instance;
    private ProgressActivity currentActivity;
    private final Handler mainHandler;

    private int currentProgress = 0;
    private String currentMessage = "Initializing...";
    private String currentTask = "Initializing...";
    private int currentItems = 0;
    private int totalItems = 0;
    private boolean isFinished = false;

    private ProgressManager() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ProgressManager getInstance() {
        if (instance == null) {
            instance = new ProgressManager();
        }
        return instance;
    }

    public void startProgress(Context context, String title, String subtitle) {
        reset();
        Intent intent = new Intent(context, ProgressActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("message", subtitle);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    /**
     * Register the progress activity (called from onCreate)
     */
    @MainThread
    public void registerActivity(ProgressActivity activity) {
        this.currentActivity = activity;
        if (activity != null) {
            activity.reset();
            activity.setProgressBar(currentProgress);
            activity.setMessage(currentMessage);
            activity.setCurrentTask(currentTask);
            activity.setItemsCount(currentItems, totalItems);
        }
    }

    /**
     * Unregister the progress activity (called from onDestroy)
     */
    @MainThread
    public void unregisterActivity() {
        this.currentActivity = null;
    }

    /**
     * Update the progress bar (0-100)
     * Thread-safe: can be called from any thread
     */
    public void updateProgress(int progress) {
        progress = Math.max(0, Math.min(100, progress));
        currentProgress = progress;

        int finalProgress = progress;
        mainHandler.post(() -> {
            if (currentActivity != null) {
                currentActivity.setProgressBar(finalProgress);
            }
        });
    }

    /**
     * Update the status message
     * Thread-safe: can be called from any thread
     */
    public void updateMessage(String message) {
        if (message == null || message.isEmpty()) return;
        currentMessage = message;
        
        mainHandler.post(() -> {
            if (currentActivity != null) {
                currentActivity.setMessage(message);
            }
        });
    }

    /**
     * Update the current task being processed
     * Thread-safe: can be called from any thread
     */
    public void setCurrentTask(String task) {
        if (task == null || task.isEmpty()) return;
        currentTask = task;
        
        mainHandler.post(() -> {
            if (currentActivity != null) {
                currentActivity.setCurrentTask(task);
            }
        });
    }

    /**
     * Update items count (current/total)
     * Thread-safe: can be called from any thread
     */
    public void setItemsCount(int current, int total) {
        currentItems = current;
        totalItems = total;
        
        mainHandler.post(() -> {
            if (currentActivity != null) {
                currentActivity.setItemsCount(current, total);
            }
        });
    }

    /**
     * Increment the progress bar by delta
     * Thread-safe: can be called from any thread
     */
    public void incrementProgress(int delta) {
        updateProgress(currentProgress + delta);
    }

    /**
     * Increment items count
     * Thread-safe: can be called from any thread
     */
    public void incrementItems() {
        setItemsCount(currentItems + 1, totalItems);
    }

    /**
     * Mark progress as finished and close the activity
     * Thread-safe: can be called from any thread
     */
    public void finishProgress() {
        isFinished = true;
        mainHandler.post(() -> {
            if (currentActivity != null) {
                currentActivity.finish();
            }
        });
    }

    /**
     * Check if progress is finished
     */
    public boolean isFinished() {
        return isFinished;
    }

    /**
     * Reset all progress values
     */
    public synchronized void reset() {
        currentProgress = 0;
        currentMessage = "Initializing...";
        currentTask = "Initializing...";
        currentItems = 0;
        totalItems = 0;
        isFinished = false;
    }

    /**
     * Get current progress value
     */
    public int getCurrentProgress() {
        return currentProgress;
    }

    /**
     * Get current message
     */
    public String getCurrentMessage() {
        return currentMessage;
    }

    /**
     * Get current task
     */
    public String getCurrentTask() {
        return currentTask;
    }

    /**
     * Get current items count
     */
    public int getCurrentItems() {
        return currentItems;
    }

    /**
     * Get total items count
     */
    public int getTotalItems() {
        return totalItems;
    }
}