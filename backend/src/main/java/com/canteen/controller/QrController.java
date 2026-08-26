package com.canteen.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/qr")
@Tag(name = "QR", description = "Generate QR codes for canteen URL")
public class QrController {

    @GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Generate a PNG QR for a given canteen URL")
    public byte[] generate(@RequestParam(defaultValue = "main") String canteenId,
                            @RequestParam(defaultValue = "300") int size,
                            @RequestParam(required = false) String baseUrl) throws Exception {
        String url = (baseUrl != null ? baseUrl : "http://localhost:5173") + "/qr/" + canteenId;
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}