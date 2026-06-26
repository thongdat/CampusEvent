package com.example.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nhận ảnh tải lên từ máy người dùng (Department Console / Admin).
 *
 * - Lưu file vào thư mục cấu hình {@code app.upload.dir} (mặc định ./uploads).
 * - Trả về URL công khai {@code /api/uploads/<tên-file>} để gắn vào proposal/event.
 * - Kiểm tra file THẬT SỰ là ảnh bằng "magic bytes" (không chỉ tin vào phần mở rộng
 *   hoặc Content-Type do client gửi) để tránh bị tải lên file độc hại.
 */
@RestController
@RequestMapping("/admin/uploads")
public class UploadController {

    /** Giới hạn 5MB cho mỗi ảnh (khớp với spring.servlet.multipart.max-file-size). */
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path uploadDir;

    public UploadController(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn tệp ảnh.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh vượt quá 5MB, vui lòng chọn ảnh nhỏ hơn.");
        }

        byte[] head = readHead(file);
        String ext = detectImageExtension(head);
        if (ext == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tệp không phải là ảnh hợp lệ (chỉ chấp nhận JPG, PNG, GIF, WEBP, BMP).");
        }

        try {
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = uploadDir.resolve(filename);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("url", "/api/uploads/" + filename);
            return ResponseEntity.ok(body);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu ảnh: " + e.getMessage());
        }
    }

    private byte[] readHead(MultipartFile file) {
        try (var in = file.getInputStream()) {
            byte[] buf = new byte[16];
            int read = in.readNBytes(buf, 0, buf.length);
            if (read < buf.length) {
                byte[] trimmed = new byte[Math.max(read, 0)];
                System.arraycopy(buf, 0, trimmed, 0, trimmed.length);
                return trimmed;
            }
            return buf;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không đọc được nội dung tệp.");
        }
    }

    /**
     * Nhận diện loại ảnh qua chữ ký nhị phân (magic bytes). Trả về phần mở rộng
     * tương ứng, hoặc {@code null} nếu không phải ảnh được hỗ trợ.
     */
    private String detectImageExtension(byte[] b) {
        if (b == null) {
            return null;
        }
        // JPEG: FF D8 FF
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return "png";
        }
        // GIF: "GIF87a" hoặc "GIF89a"
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8'
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') {
            return "gif";
        }
        // BMP: "BM"
        if (b.length >= 2 && b[0] == 'B' && b[1] == 'M') {
            return "bmp";
        }
        // WEBP: "RIFF" .... "WEBP"
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
