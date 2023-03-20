package com.mss.totaldiff;

import com.mss.totaldiff.visitors.ItemVisitor;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileItem extends FileItemBase {
    public long size;
    public String extension = "";
    public String fileDigest = "";

    public FileItem(int aId, String aName, DirItem aParentDir) {
        super(aId,aName, aParentDir);
    }

    public static long totalHashTimeNanos = 0;

    private static final char[] hexChars = "0123456789abcdef".toCharArray();

    @Override
    public void visitItem(ItemVisitor visitor) {
        visitor.processFileItem(this);
    }

    private String fastHex(byte[] digest) {
        char[] hexBuffer = new char[digest.length * 2];
        int intValue;
        for(int i = 0, j = 0; i < 16; i++) {
            intValue = digest[i] & 0x00ff;
            hexBuffer[j++] = hexChars[intValue >> 4];
            hexBuffer[j++] = hexChars[intValue & 0x0f];
        }
        return new String(hexBuffer);
    }

    public void computeHash2(File file, int bufferSize) throws IOException, NoSuchAlgorithmException {
        long startTime = System.nanoTime();
        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] buffer = new byte[bufferSize];
        int readSize;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            readSize = inputStream.read(buffer);
            while (readSize >= 0) {
                md.update(buffer, 0, readSize);
                readSize = inputStream.read(buffer);
            }
            fileDigest = fastHex(md.digest());
        }
        totalHashTimeNanos += (System.nanoTime() - startTime);
    }

    public void computeHash(File file, int bufferSize, long maxSize) throws IOException, NoSuchAlgorithmException {
        long startTime = System.nanoTime();
        byte[] buffer = new byte[bufferSize];
        byte[] hash = new byte[16];
        int hashIndex = 0;
        long totalReadSize = 0;
        int readSize;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            readSize = inputStream.read(buffer);
            while (readSize >= 0) {
                for(int i = 0; i < readSize && totalReadSize < maxSize; i++) {
                    totalReadSize++;
                    hash[hashIndex++] ^= buffer[i];
                    if (hashIndex == 16) hashIndex = 0;
                }
                if (totalReadSize >= maxSize) break;
                readSize = inputStream.read(buffer);
            }
            fileDigest = fastHex(hash);
        }
        totalHashTimeNanos += (System.nanoTime() - startTime);
    }


    public String getKeyName() {
        return extension + "-" + fileDigest + size;
    }


    @Override
    public String toString() {
        return String.format("%2d: under dir %d with name %s of size %d with extension %s", id, parentDir.id, name, size, extension);
    }

    public void printLine() {
        System.out.println(this.toString());
    }
}
