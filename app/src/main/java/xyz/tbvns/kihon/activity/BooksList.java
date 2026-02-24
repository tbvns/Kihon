package xyz.tbvns.kihon.activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import xyz.tbvns.kihon.R;
import xyz.tbvns.kihon.databinding.ActivityBooksListBinding;
import xyz.tbvns.kihon.logic.FilesLogic;
import xyz.tbvns.kihon.logic.Object.ChapterObject;
import xyz.tbvns.kihon.ui.convert.ChapterAdapter;
import xyz.tbvns.kihon.ui.convert.SourceAdapter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class BooksList extends AppCompatActivity {
    private ActivityBooksListBinding binding;
    private final ArrayList<String> currentItems = new ArrayList<>();
    private final Stack<String> folderStack = new Stack<>();
    private final HashSet<String> selectedChapters = new HashSet<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBooksListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Button button = findViewById(R.id.proceedButton);
        button.setEnabled(!selectedChapters.isEmpty());

        String start = getIntent().getStringExtra("folder_name");
        if (start == null) { finish(); return; }

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.sourcesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadFolder(start, true);
    }

    private void loadFolder(String folderName, boolean isPush) {
        if (isPush) folderStack.push(folderName);

        executor.execute(() -> {
            ArrayList<String> items = new ArrayList<>();
            boolean isChapterView = folderStack.size() >= 2;

            if (isChapterView) {
                List<ChapterObject> chapters = FilesLogic.listChapters(folderStack.get(0), folderStack.peek());
                for (ChapterObject c : chapters) items.add(c.title != null ? c.title : String.valueOf(c.number));
            } else {
                items = FilesLogic.listFolder(folderName);
            }

            ArrayList<String> finalItems = items;
            mainHandler.post(() -> updateUI(finalItems, isChapterView));
        });
    }

    private void updateUI(ArrayList<String> items, boolean isChapterView) {
        currentItems.clear();
        currentItems.addAll(items);

        if (isChapterView) {
            binding.sourcesRecyclerView.setAdapter(new ChapterAdapter(currentItems, selectedChapters, (name, isSelected) -> {
                if (isSelected) selectedChapters.add(name);
                else selectedChapters.remove(name);

                Button button = findViewById(R.id.proceedButton);
                button.setEnabled(!selectedChapters.isEmpty());
            }));
        } else {
            binding.sourcesRecyclerView.setAdapter(new SourceAdapter(currentItems, name -> loadFolder(name, true)));
        }
        updateToolbarTitle();
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