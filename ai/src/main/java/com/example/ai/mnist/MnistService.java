package com.example.ai.mnist;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class MnistService {

    private static final Logger log = LoggerFactory.getLogger(MnistService.class);
    private static final Path MODEL_PATH = Path.of("mnist-model.ser");

    private MnistNeuralNetwork network;
    private volatile boolean training = false;
    private String trainingStatus = "idle";

    @PostConstruct
    public void init() {
        if (Files.exists(MODEL_PATH)) {
            try {
                network = MnistNeuralNetwork.load(MODEL_PATH.toString());
                log.info("Model loaded from {}", MODEL_PATH.toAbsolutePath());
                trainingStatus = "ready";
            } catch (Exception e) {
                log.warn("Failed to load model, creating new one: {}", e.getMessage());
                network = new MnistNeuralNetwork();
                trainingStatus = "untrained";
            }
        } else {
            network = new MnistNeuralNetwork();
            trainingStatus = "untrained";
            log.info("No saved model found. Call /api/mnist/train to train.");
        }
    }

    /** 预测数字 0-9，返回各类别概率 */
    public double[] predict(double[] pixels) {
        if (pixels.length != 784) throw new IllegalArgumentException("Expected 784 pixels (28x28)");
        return network.predictProbabilities(pixels);
    }

    /** 预测数字标签 */
    public int predictDigit(double[] pixels) {
        return network.predict(pixels);
    }

    /** 异步训练 */
    public void trainAsync(int epochs, double learningRate) {
        if (training) throw new IllegalStateException("Training already in progress");
        training = true;
        trainingStatus = "training";

        new Thread(() -> {
            try {
                log.info("Loading training data...");
                var trainData = MnistDataLoader.loadTrainingData();
                log.info("Training set: {} samples", trainData.images().length);

                for (int epoch = 0; epoch < epochs; epoch++) {
                    double totalLoss = 0;
                    for (int i = 0; i < trainData.images().length; i++) {
                        totalLoss += network.trainOne(trainData.images()[i], trainData.labels()[i], learningRate);
                    }
                    double avgLoss = totalLoss / trainData.images().length;
                    log.info("Epoch {}/{} — avg loss: {}", epoch + 1, epochs, String.format("%.4f", avgLoss));
                    trainingStatus = String.format("training (epoch %d/%d)", epoch + 1, epochs);
                }

                log.info("Evaluating on test set...");
                var testData = MnistDataLoader.loadTestData();
                double acc = network.evaluate(testData.images(), testData.labels());
                log.info("Test accuracy: {}%", String.format("%.2f", acc * 100));

                network.save(MODEL_PATH.toString());
                trainingStatus = String.format("ready (accuracy: %.2f%%)", acc * 100.0);
            } catch (Exception e) {
                log.error("Training failed", e);
                trainingStatus = "failed: " + e.getMessage();
            } finally {
                training = false;
            }
        }, "mnist-trainer").start();
    }

    public String getStatus() { return trainingStatus; }
    public boolean isTrained() { return trainingStatus.startsWith("ready"); }
}
