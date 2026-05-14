package com.example.ai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class PdfService {

    /**
     * 给 PDF 添加文字水印，返回加水印后的 PDF 字节数组
     *
     * @param inputStream   PDF 输入流
     * @param watermarkText 水印文字
     * @return 加水印后的 PDF 字节数组
     */
    public byte[] addWatermark(InputStream inputStream, String watermarkText) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes());
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            for (PDPage page : document.getPages()) {
                addWatermarkToPage(document, page, watermarkText);
            }

            document.save(bos);
            return bos.toByteArray();
        }
    }

    /**
     * 给单个页面添加对角水印
     */
    private void addWatermarkToPage(PDDocument document, PDPage page, String text) throws IOException {
        PDRectangle mediaBox = page.getMediaBox();
        float pageWidth = mediaBox.getWidth();
        float pageHeight = mediaBox.getHeight();
        float centerX = pageWidth / 2;
        float centerY = pageHeight / 2;

        // 字体大小按页面宽度比例缩放，适合大部分 PDF
        float fontSize = pageWidth / 15;
        PDFont font;
        try {
            font = PDType0Font.load(document,
                    getClass().getResourceAsStream("/fonts/NotoSansSC-Regular.ttf"));
        } catch (Exception e) {
            // 回退到标准字体（不支持中文水印）
            font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }

        try (PDPageContentStream cs = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

            // === 设置透明度 ===
            PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
            gs.setNonStrokingAlphaConstant(0.15f);
            gs.setStrokingAlphaConstant(0.15f);
            cs.setGraphicsStateParameters(gs);

            // === 设置字体和颜色（灰色） ===
            cs.setFont(font, fontSize);
            cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);  // 深灰
            cs.setStrokingColor(0.4f, 0.4f, 0.4f);

            // === 画多个水印形成平铺效果 ===
            float textWidth = font.getStringWidth(text) / 1000 * fontSize;
            float stepX = textWidth * 2.0f;
            float stepY = fontSize * 5.0f;

            for (float y = -pageHeight; y < pageHeight * 2; y += stepY) {
                for (float x = -pageWidth; x < pageWidth * 2; x += stepX) {
                    // 每一行交替偏移
                    float offsetX = (int) (y / stepY) % 2 == 0 ? 0 : textWidth;

                    cs.beginText();
                    // 设置旋转 45 度的矩阵
                    cs.setTextMatrix(Matrix.getRotateInstance(
                            Math.toRadians(45),
                            x + offsetX,
                            y
                    ));
                    cs.showText(text);
                    cs.endText();
                }
            }

            // === 正中央大号水印 ===
            float bigFontSize = pageWidth / 5;
            cs.setFont(font, bigFontSize);
            float bigTextWidth = font.getStringWidth(text) / 1000 * bigFontSize;

            gs.setNonStrokingAlphaConstant(0.08f);
            gs.setStrokingAlphaConstant(0.08f);
            cs.setGraphicsStateParameters(gs);

            cs.beginText();
            cs.setTextMatrix(Matrix.getRotateInstance(
                    Math.toRadians(45),
                    centerX - bigTextWidth / 2,
                    centerY - bigFontSize / 2
            ));
            cs.showText(text);
            cs.endText();
        }
    }
}
