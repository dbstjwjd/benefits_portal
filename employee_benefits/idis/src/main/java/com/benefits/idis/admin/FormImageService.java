package com.benefits.idis.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 이미지 선택지에 붙는 그림 파일. DB 에는 경로만 남기고 실제 파일은 디스크에 둔다.
 * 저장 위치는 app.upload.dir 로 바꿀 수 있고, 화면에서는 /uploads/... 로 읽는다.
 */
@Service
public class FormImageService {

    public static final long MAX_BYTES = 5 * 1024 * 1024;
    /** 브라우저로 되돌려 주는 파일이라 확장자만 믿지 않고 앞부분 바이트까지 본다. */
    private static final String JPEG = "jpg";
    private static final String PNG = "png";
    private static final String WEBP = "webp";

    private static final String WEB_PREFIX = "/uploads/forms/";
    private static final String SUB_DIR = "forms";

    private final Path root;

    public FormImageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    /** 저장에 성공하면 화면에서 쓸 경로를 돌려준다. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지를 선택해주세요");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("이미지는 5MB 이하만 올릴 수 있습니다");
        }

        String extension = sniff(file);
        String name = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path directory = root.resolve(SUB_DIR);

        try {
            Files.createDirectories(directory);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지를 저장하지 못했습니다");
        }
        return WEB_PREFIX + name;
    }

    /** 앞 12 바이트로 형식을 판단한다. jpg / png / webp 가 아니면 거절한다. */
    private static String sniff(MultipartFile file) {
        byte[] head = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(head, 0, head.length);
            if (read < head.length) {
                throw new IllegalArgumentException("jpg, png, webp 이미지만 올릴 수 있습니다");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지를 읽지 못했습니다");
        }

        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return PNG;
        }
        if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return WEBP;
        }
        throw new IllegalArgumentException("jpg, png, webp 이미지만 올릴 수 있습니다");
    }
}
