package xyz.tbvns.kihon.logic;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sorter {

    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "^(\\d+)(?:__(\\d+))?\\.\\w+$"
    );

    public static class ParsedFileName {
        public int major;
        public int minor;
        public String originalName;

        public ParsedFileName(int major, int minor, String originalName) {
            this.major = major;
            this.minor = minor;
            this.originalName = originalName;
        }
    }

    public static class ChapterGroup {
        public int chapterIndex;
        public List<DocumentFile> files;

        public ChapterGroup(int chapterIndex) {
            this.chapterIndex = chapterIndex;
            this.files = new ArrayList<>();
        }
    }

    public static ParsedFileName parseFileName(String fileName) {
        String baseName = new File(fileName).getName();

        Matcher matcher = FILENAME_PATTERN.matcher(baseName);
        if (!matcher.matches()) {
            return null;
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;

        return new ParsedFileName(major, minor, baseName);
    }

    public static List<DocumentFile> sortByChapterThenByMajorMinor(
            List<DocumentFile> allFiles,
            List<Integer> chapterBoundaries) {

        List<DocumentFile> result = new ArrayList<>();

        for (int chapterIdx = 0; chapterIdx < chapterBoundaries.size(); chapterIdx++) {
            int startIdx = chapterBoundaries.get(chapterIdx);
            int endIdx = (chapterIdx + 1 < chapterBoundaries.size())
                    ? chapterBoundaries.get(chapterIdx + 1)
                    : allFiles.size();

            List<DocumentFile> chapterFiles = new ArrayList<>(
                    allFiles.subList(startIdx, endIdx)
            );

            chapterFiles.sort((f1, f2) -> {
                String name1 = f1.getName();
                String name2 = f2.getName();

                if (name1 == null || name2 == null) {
                    return 0;
                }

                ParsedFileName parsed1 = parseFileName(name1);
                ParsedFileName parsed2 = parseFileName(name2);

                if (parsed1 == null || parsed2 == null) {
                    return name1.compareTo(name2);
                }

                if (parsed1.major != parsed2.major) {
                    return Integer.compare(parsed1.major, parsed2.major);
                }
                return Integer.compare(parsed1.minor, parsed2.minor);
            });

            result.addAll(chapterFiles);
        }

        return result;
    }
}