package com.example.ai.mnist;

import java.io.*;
import java.util.Random;

/**
 * 三层全连接神经网络：784(输入) → 128(隐藏,ReLU) → 10(输出,Softmax)
 * 使用交叉熵损失 + 小批量 SGD 训练
 */
public class MnistNeuralNetwork implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int inputSize = 784;
    private final int hiddenSize = 128;
    private final int outputSize = 10;

    private double[][] W1; // hiddenSize x inputSize
    private double[] b1;   // hiddenSize
    private double[][] W2; // outputSize x hiddenSize
    private double[] b2;   // outputSize

    private final Random rng = new Random(42);

    public MnistNeuralNetwork() {
        // He 初始化
        double he1 = Math.sqrt(2.0 / inputSize);
        double he2 = Math.sqrt(2.0 / hiddenSize);

        W1 = new double[hiddenSize][inputSize];
        for (int i = 0; i < hiddenSize; i++)
            for (int j = 0; j < inputSize; j++)
                W1[i][j] = rng.nextGaussian() * he1;

        b1 = new double[hiddenSize];

        W2 = new double[outputSize][hiddenSize];
        for (int i = 0; i < outputSize; i++)
            for (int j = 0; j < hiddenSize; j++)
                W2[i][j] = rng.nextGaussian() * he2;

        b2 = new double[outputSize];
    }

    /** 前向传播，返回各层激活值 */
    public ForwardResult forward(double[] x) {
        // 隐藏层: z1 = W1·x + b1, a1 = ReLU(z1)
        double[] z1 = new double[hiddenSize];
        double[] a1 = new double[hiddenSize];
        for (int i = 0; i < hiddenSize; i++) {
            double sum = b1[i];
            for (int j = 0; j < inputSize; j++)
                sum += W1[i][j] * x[j];
            z1[i] = sum;
            a1[i] = Math.max(0, sum); // ReLU
        }

        // 输出层: z2 = W2·a1 + b2, a2 = softmax(z2)
        double[] z2 = new double[outputSize];
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < outputSize; i++) {
            double sum = b2[i];
            for (int j = 0; j < hiddenSize; j++)
                sum += W2[i][j] * a1[j];
            z2[i] = sum;
            if (sum > maxZ) maxZ = sum;
        }

        double[] a2 = new double[outputSize];
        double sumExp = 0;
        for (int i = 0; i < outputSize; i++) {
            a2[i] = Math.exp(z2[i] - maxZ);
            sumExp += a2[i];
        }
        for (int i = 0; i < outputSize; i++)
            a2[i] /= sumExp;

        return new ForwardResult(z1, a1, z2, a2);
    }

    /** 单样本训练（在线学习），返回损失 */
    public double trainOne(double[] x, int label, double learningRate) {
        ForwardResult f = forward(x);

        // 输出层误差: dL/dz2 = a2 - onehot
        double[] dz2 = f.a2.clone();
        dz2[label] -= 1.0;

        // 隐藏层误差: dL/dz1 = (W2^T · dz2) ⊙ ReLU'(z1)
        double[] dz1 = new double[hiddenSize];
        for (int i = 0; i < hiddenSize; i++) {
            double sum = 0;
            for (int j = 0; j < outputSize; j++)
                sum += W2[j][i] * dz2[j];
            dz1[i] = f.z1[i] > 0 ? sum : 0;
        }

        // 梯度更新 W2, b2
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < hiddenSize; j++)
                W2[i][j] -= learningRate * dz2[i] * f.a1[j];
            b2[i] -= learningRate * dz2[i];
        }

        // 梯度更新 W1, b1
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < inputSize; j++)
                W1[i][j] -= learningRate * dz1[i] * x[j];
            b1[i] -= learningRate * dz1[i];
        }

        return -Math.log(Math.max(f.a2[label], 1e-15));
    }

    /** 预测，返回 0-9 的数字 */
    public int predict(double[] x) {
        ForwardResult f = forward(x);
        int best = 0;
        for (int i = 1; i < outputSize; i++)
            if (f.a2[i] > f.a2[best]) best = i;
        return best;
    }

    /** 预测，返回各类别概率 */
    public double[] predictProbabilities(double[] x) {
        return forward(x).a2;
    }

    /** 在测试集上评估准确率 */
    public double evaluate(double[][] images, int[] labels) {
        int correct = 0;
        for (int i = 0; i < images.length; i++) {
            if (predict(images[i]) == labels[i]) correct++;
        }
        return (double) correct / images.length;
    }

    /** 保存模型到文件 */
    public void save(String path) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(this);
        }
    }

    /** 从文件加载模型 */
    public static MnistNeuralNetwork load(String path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            return (MnistNeuralNetwork) ois.readObject();
        }
    }

    public record ForwardResult(double[] z1, double[] a1, double[] z2, double[] a2) {}
}
