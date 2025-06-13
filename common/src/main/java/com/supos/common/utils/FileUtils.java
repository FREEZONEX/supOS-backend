package com.supos.common.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils {

    public static boolean deleteDir(File file) {
        if (!file.exists()) {
            return false;
        }
        Path path = file.toPath();
        boolean delDir = file.delete();
        if (!delDir) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                return false;
            }
            if (file.exists()) {
                delDir = file.delete();
            }
        }
        return delDir;
    }

}
