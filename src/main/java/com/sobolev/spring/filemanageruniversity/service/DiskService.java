package com.sobolev.spring.filemanageruniversity.service;

import com.sobolev.spring.filemanageruniversity.config.FileManagerConstants;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.DecimalFormat;


@Service
public class DiskService {

    public String getDisksInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Информация о дисках ===\n\n");

        File[] roots = File.listRoots();
        DecimalFormat df = new DecimalFormat("#.##");

        for (File root : roots) {
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;

            info.append("Диск: ").append(root.getAbsolutePath()).append("\n");
            info.append("  Общий объем: ").append(formatBytes(totalSpace)).append("\n");
            info.append("  Свободно: ").append(formatBytes(freeSpace)).append("\n");
            info.append("  Использовано: ").append(formatBytes(usedSpace)).append("\n");
            
            if (totalSpace > 0) {
                double percentUsed = (double) usedSpace / totalSpace * 100;
                info.append("  Использовано: ").append(df.format(percentUsed)).append("%\n");
            }
            info.append("\n");
        }

        return info.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes < FileManagerConstants.BYTES_PER_KB) {
            return bytes + " B";
        }
        if (bytes < FileManagerConstants.BYTES_PER_MB) {
            return String.format("%.2f KB", bytes / (double) FileManagerConstants.BYTES_PER_KB);
        }
        if (bytes < FileManagerConstants.BYTES_PER_GB) {
            return String.format("%.2f MB", bytes / (double) FileManagerConstants.BYTES_PER_MB);
        }
        return String.format("%.2f GB", bytes / (double) FileManagerConstants.BYTES_PER_GB);
    }
}

