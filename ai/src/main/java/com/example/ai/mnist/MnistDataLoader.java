package com.example.ai.mnist;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

/**
 * MNIST 数据集加载器，支持自动下载和 IDX 格式解析
 */
public class MnistDataLoader {

    private static final String BASE_URL = "https://github.com/cvdfoundation/mnist/raw/master/data/";
    private static final String[] FILES = {
            "train-images-idx3-ubyte.gz", "train-labels-idx1-ubyte.gz",
            "t10k-images-idx3-ubyte.gz",  "t10k-labels-idx1-ubyte.gz"
    };

    public record MnistDataset(double[][] images, int[] labels) {}

    /** 加载训练集（自动下载到临时目录） */
    public static MnistDataset loadTrainingData() throws IOException {
        return loadData(FILES[0], FILES[1]);
    }

    /** 加载测试集 */
    public static MnistDataset loadTestData() throws IOException {
        return loadData(FILES[2], FILES[3]);
    }

    private static MnistDataset loadData(String imageFile, String labelFile) throws IOException {
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), "mnist");
        Files.createDirectories(dir);

        Path imgPath = dir.resolve(imageFile.replace(".gz", ""));
        Path lblPath = dir.resolve(labelFile.replace(".gz", ""));

        if (!Files.exists(imgPath)) downloadAndExtract(imageFile, imgPath, dir);
        if (!Files.exists(lblPath)) downloadAndExtract(labelFile, lblPath, dir);

        double[][] images = readImages(imgPath);
        int[] labels = readLabels(lblPath);
        return new MnistDataset(images, labels);
    }

    private static void downloadAndExtract(String fileName, Path dest, Path dir) throws IOException {
        Path gzPath = dir.resolve(fileName);
        if (!Files.exists(gzPath)) {
            System.out.println("Downloading " + fileName + " ...");
            try (InputStream in = new URL(BASE_URL + fileName).openStream()) {
                Files.copy(in, gzPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        System.out.println("Extracting " + fileName + " ...");
        try (GZIPInputStream gzIn = new GZIPInputStream(new FileInputStream(gzPath.toFile()));
             FileOutputStream fos = new FileOutputStream(dest.toFile())) {
            gzIn.transferTo(fos);
        }
    }

    /** 读取 IDX3 图像文件，返回归一化到 [0,1] 的 double 数组 */
    private static double[][] readImages(Path path) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path.toFile())))) {
            int magic = dis.readInt();
            if (magic != 0x803) throw new IOException("Invalid image magic: " + Integer.toHexString(magic));
            int count = dis.readInt();
            int rows = dis.readInt();
            int cols = dis.readInt();
            int pixels = rows * cols;

            double[][] images = new double[count][pixels];
            byte[] buf = new byte[pixels];
            for (int i = 0; i < count; i++) {
                dis.readFully(buf);
                double[] img = images[i];
                for (int j = 0; j < pixels; j++)
                    img[j] = (buf[j] & 0xFF) / 255.0;
            }
            return images;
        }
    }

    /** 读取 IDX1 标签文件 */
    private static int[] readLabels(Path path) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path.toFile())))) {
            int magic = dis.readInt();
            if (magic != 0x801) throw new IOException("Invalid label magic: " + Integer.toHexString(magic));
            int count = dis.readInt();
            int[] labels = new int[count];
            byte[] buf = new byte[count];
            dis.readFully(buf);
            for (int i = 0; i < count; i++)
                labels[i] = buf[i] & 0xFF;
            return labels;
        }
    }
}
