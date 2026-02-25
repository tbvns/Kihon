package xyz.tbvns.kihon.activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import xyz.tbvns.kihon.R;
import xyz.tbvns.kihon.databinding.ActivityBooksListBinding;
import xyz.tbvns.kihon.logic.FilesLogic;
import xyz.tbvns.kihon.logic.Object.BrowserItem;
import xyz.tbvns.kihon.logic.Object.ChapterObject;
import xyz.tbvns.kihon.ui.convert.ChapterAdapter;
import xyz.tbvns.kihon.ui.convert.SourceAdapter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class BooksList extends AppCompatActivity {
    private ActivityBooksListBinding binding;
    private final ArrayList<BrowserItem> currentItems = new ArrayList<>();
    private final ArrayList<ChapterObject> selectedChapters = new ArrayList<>();
    private final Stack<String> folderStack = new Stack<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBooksListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Button button = findViewById(R.id.proceedButton);
        button.setEnabled(false);

        button.setOnClickListener(l ->{
            ExportOptionsActivity.ExportOptionsLauncher.pendingFiles = selectedChapters;
            ExportOptionsActivity.ExportOptionsLauncher.pendingReEncode = true;

            Intent intent = new Intent(this, ExportOptionsActivity.class);
            startActivity(intent);
        });

        FloatingActionButton selectAll = findViewById(R.id.selectAll);
        selectAll.setEnabled(false);

        selectAll.setOnClickListener(l -> {
            ArrayList<ChapterObject> usableCurrent = currentItems.stream().map(BrowserItem::getChapter).collect(Collectors.toCollection(ArrayList::new));
            if (selectedChapters.containsAll(usableCurrent)) {
                usableCurrent.forEach(selectedChapters::remove);
            } else {
                selectedChapters.addAll(usableCurrent);
            }
            updateSelectAllIcon(selectAll);
            updateProceedButton();
            refreshChapterAdapter();
        });

        String start = getIntent().getStringExtra("folder_name");
        if (start == null) { finish(); return; }

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.sourcesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadFolder(start, true);
    }

    private void loadFolder(String folderName, boolean isPush) {
        if (isPush) folderStack.push(folderName);

        executor.execute(() -> {
            ArrayList<BrowserItem> items = new ArrayList<>();
            boolean isChapterView = folderStack.size() >= 2;

            if (isChapterView) {
                List<ChapterObject> chapters = FilesLogic.listChapters(folderStack.get(0), folderStack.peek());
                for (ChapterObject c : chapters) {
                    items.add(new BrowserItem(c));
                }
            } else {
                ArrayList<String> folders = FilesLogic.listFolder(folderName);
                for (String folder : folders) {
                    items.add(new BrowserItem(folder));
                }
            }

            mainHandler.post(() -> updateUI(items, isChapterView));
        });
    }

    private void updateUI(ArrayList<BrowserItem> items, boolean isChapterView) {
        ArrayList<ChapterObject> usableCurrent = currentItems.stream().map(BrowserItem::getChapter).collect(Collectors.toCollection(ArrayList::new));
        currentItems.clear();
        currentItems.addAll(items);
        selectedChapters.retainAll(usableCurrent);

        if (isChapterView) {
            refreshChapterAdapter();
        } else {
            binding.sourcesRecyclerView.setAdapter(
                    new SourceAdapter(currentItems, name -> loadFolder(name, true))
            );
        }

        updateProceedButton();
        updateSelectAllIcon(findViewById(R.id.selectAll));
        updateToolbarTitle();
    }

    private void refreshChapterAdapter() {
        binding.sourcesRecyclerView.setAdapter(new ChapterAdapter(currentItems, selectedChapters, (name, isSelected) -> {
            if (isSelected) selectedChapters.add(name);
            else selectedChapters.remove(name);

            updateProceedButton();
            updateSelectAllIcon(findViewById(R.id.selectAll));
        }));
    }

    private void updateProceedButton() {
        Button button = findViewById(R.id.proceedButton);
        button.setEnabled(!selectedChapters.isEmpty());

        FloatingActionButton fab = findViewById(R.id.selectAll);
        fab.setEnabled(folderStack.size() >= 2);
    }

    private void updateSelectAllIcon(FloatingActionButton selectAll) {
        ArrayList<ChapterObject> usableCurrent = currentItems.stream().map(BrowserItem::getChapter).collect(Collectors.toCollection(ArrayList::new));
        if (!currentItems.isEmpty() && selectedChapters.containsAll(usableCurrent)) {
            selectAll.setImageResource(R.drawable.deselect);
        } else {
            selectAll.setImageResource(R.drawable.select_all);
        }
    }

    private void updateToolbarTitle() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(String.join(" / ", folderStack));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (folderStack.size() > 1) {
            folderStack.pop();
            loadFolder(folderStack.peek(), false);
            return true;
        }
        return super.onSupportNavigateUp();
    }
}