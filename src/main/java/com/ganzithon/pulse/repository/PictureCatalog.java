package com.ganzithon.pulse.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganzithon.pulse.entity.Mode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 숨은그림 카탈로그. 그림은 DB가 아니라 classpath:pictures/catalog.json에 두고, 기동할 때 한 번 읽어 검사한다.
 * 데이터가 규칙을 어기면 빈 생성이 실패해 서버가 뜨지 않는다.
 */
@Component
public class PictureCatalog {
    private static final String CATALOG_PATH = "pictures/catalog.json";

    /** 이미지 너비 / 높이. image는 static/pictures/ 안의 파일 이름 */
    public record Picture(String pictureId, Mode mode, String image, double aspectRatio, List<Target> targets) {}

    /** x, y는 이미지 너비·높이 기준 비율, radius는 이미지 너비 기준 비율 */
    public record Target(String targetId, String label, String emoji, double x, double y, double radius) {}

    private final Map<Mode, List<Picture>> byMode;

    @Autowired
    public PictureCatalog(ObjectMapper objectMapper) {
        this(read(objectMapper));
    }

    /** 테스트에서 Spring 없이 목록을 직접 넘길 때 쓴다 */
    public PictureCatalog(List<Picture> pictures) {
        validate(pictures);
        Map<Mode, List<Picture>> grouped = new EnumMap<>(Mode.class);
        for (Mode mode : Mode.values()) {
            grouped.put(mode, pictures.stream().filter(p -> p.mode() == mode).toList());
        }
        this.byMode = Map.copyOf(grouped);
    }

    public List<Picture> findByMode(Mode mode) {
        return byMode.get(mode);
    }

    static List<Picture> read(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(CATALOG_PATH).getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<Picture>>() {});
        } catch (IOException e) {
            throw new IllegalStateException(CATALOG_PATH + "를 읽지 못했습니다: " + e.getMessage(), e);
        }
    }

    public static void validate(List<Picture> pictures) {
        Set<String> pictureIds = new HashSet<>();
        for (Picture p : pictures) {
            String id = p.pictureId();
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("pictureId가 비어 있습니다");
            }
            if (!pictureIds.add(id)) {
                throw new IllegalStateException(id + ": pictureId가 중복입니다");
            }
            if (p.mode() == null) {
                throw new IllegalStateException(id + ": mode가 없습니다");
            }
            if (p.image() == null || p.image().isBlank()) {
                throw new IllegalStateException(id + ": image가 비어 있습니다");
            }
            if (!(p.aspectRatio() > 0)) {
                throw new IllegalStateException(id + ": aspectRatio는 0보다 커야 합니다 (" + p.aspectRatio() + ")");
            }
            if (p.targets() == null || p.targets().isEmpty()) {
                throw new IllegalStateException(id + ": targets는 1개 이상이어야 합니다");
            }
            Set<String> targetIds = new HashSet<>();
            for (Target t : p.targets()) {
                String where = id + " / " + t.targetId();
                if (t.targetId() == null || t.targetId().isBlank()) {
                    throw new IllegalStateException(id + ": targetId가 비어 있습니다");
                }
                if (!targetIds.add(t.targetId())) {
                    throw new IllegalStateException(where + ": targetId가 그림 안에서 중복입니다");
                }
                requireRatio(where, "x", t.x());
                requireRatio(where, "y", t.y());
                if (!(t.radius() > 0 && t.radius() <= 1)) {
                    throw new IllegalStateException(where + ": radius는 0 초과 1 이하여야 합니다 (" + t.radius() + ")");
                }
                if (t.label() == null || t.label().isBlank()) {
                    throw new IllegalStateException(where + ": label이 비어 있습니다");
                }
                if (t.emoji() == null || t.emoji().isBlank()) {
                    throw new IllegalStateException(where + ": emoji가 비어 있습니다");
                }
            }
        }
    }

    private static void requireRatio(String where, String field, double value) {
        if (!(value >= 0 && value <= 1)) {
            throw new IllegalStateException(where + ": " + field + "는 0~1이어야 합니다 (" + value + ")");
        }
    }
}
