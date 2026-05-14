package com.example.ai.controller;

import com.example.ai.dto.Result;
import com.example.ai.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final PdfService pdfService;

    @Autowired
    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    /**
     * 上传 PDF + 水印文字，返回带水印的 PDF 文件
     */
    @PostMapping("/watermark")
    public ResponseEntity<?> addWatermark(
            @RequestParam("file") MultipartFile file,
            @RequestParam("watermark") String watermarkText) {

        // 校验文件
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.error("请上传 PDF 文件"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            return ResponseEntity.badRequest().body(Result.error("仅支持 PDF 格式文件"));
        }

        // 校验水印文字
        if (watermarkText == null || watermarkText.isBlank()) {
            return ResponseEntity.badRequest().body(Result.error("水印内容不能为空"));
        }

        try {
            byte[] watermarkedPdf = pdfService.addWatermark(file.getInputStream(), watermarkText.trim());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"watermarked_" + file.getOriginalFilename() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(watermarkedPdf);

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Result.error("PDF 处理失败：" + e.getMessage()));
        }
    }
}
