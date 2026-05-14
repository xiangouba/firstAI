package com.example.ai.mnist;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mnist")
public class MnistController {

    private final MnistService mnistService;

    public MnistController(MnistService mnistService) {
        this.mnistService = mnistService;
    }

    /** 预测：接收 784 个归一化像素值，返回预测结果 */
    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestBody double[] pixels) {
        try {
            double[] probs = mnistService.predict(pixels);
            int digit = argmax(probs);
            return ResponseEntity.ok(Map.of(
                    "digit", digit,
                    "probabilities", probs,
                    "confidence", String.format("%.2f%%", probs[digit] * 100)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 触发训练 */
    @PostMapping("/train")
    public ResponseEntity<?> train(
            @RequestParam(defaultValue = "5") int epochs,
            @RequestParam(defaultValue = "0.01") double learningRate) {
        try {
            mnistService.trainAsync(epochs, learningRate);
            return ResponseEntity.ok(Map.of("message", "Training started", "epochs", epochs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 查看训练状态 */
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
                "status", mnistService.getStatus(),
                "trained", mnistService.isTrained()
        ));
    }

    private static int argmax(double[] arr) {
        int best = 0;
        for (int i = 1; i < arr.length; i++)
            if (arr[i] > arr[best]) best = i;
        return best;
    }
}
