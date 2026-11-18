package com.ganzithon.pulse.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganzithon.pulse.entity.Mode;
import com.ganzithon.pulse.repository.PictureCatalog.Picture;
import com.ganzithon.pulse.repository.PictureCatalog.Target;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Spring 없이 검사 규칙만 확인한다 */
class PictureCatalogTest {
    private static Target target(String id, double x, double y, double radius) {
        return new Target(id, "고양이", "🐱", x, y, radius);
    }

    private static Picture picture(String id, double aspectRatio, Target... targets) {
        return new Picture(id, Mode.OBJECT, "a.png", aspectRatio, List.of(targets));
    }

    private static Picture valid(String id) {
        return picture(id, 0.5, target("t1", 0.1, 0.2, 0.05), target("t2", 0.9, 0.8, 0.05));
    }

    private static void assertRejected(String pictureId, List<Picture> pictures) {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> PictureCatalog.validate(pictures));
        assertTrue(e.getMessage().contains(pictureId), e.getMessage());
    }

    @Test
    void validDataPasses() {
        assertDoesNotThrow(() -> PictureCatalog.validate(List.of(valid("p1"), valid("p2"))));
        PictureCatalog catalog = new PictureCatalog(List.of(valid("p1")));
        assertEquals(1, catalog.findByMode(Mode.OBJECT).size());
        assertTrue(catalog.findByMode(Mode.COLOR).isEmpty());
    }

    @Test
    void emptyTargetsRejected() {
        assertRejected("p_empty", List.of(picture("p_empty", 0.5)));
    }

    @Test
    void xOutOfRangeRejected() {
        assertRejected("p_x", List.of(picture("p_x", 0.5, target("t1", 1.2, 0.5, 0.05))));
    }

    @Test
    void negativeYRejected() {
        assertRejected("p_y", List.of(picture("p_y", 0.5, target("t1", 0.5, -0.1, 0.05))));
    }

    @Test
    void radiusOutOfRangeRejected() {
        assertRejected("p_r", List.of(picture("p_r", 0.5, target("t1", 0.5, 0.5, 1.5))));
    }

    @Test
    void zeroRadiusRejected() {
        // JSON에서 radius 키를 빠뜨리면 0.0으로 채워지므로 0도 막아야 한다
        assertRejected("p_r0", List.of(picture("p_r0", 0.5, target("t1", 0.5, 0.5, 0))));
    }

    @Test
    void radiusOfOneIsAllowed() {
        assertDoesNotThrow(() -> PictureCatalog.validate(List.of(picture("p_r1", 0.5, target("t1", 0.5, 0.5, 1)))));
    }

    @Test
    void blankLabelRejected() {
        for (String label : new String[] {"", "  ", null}) {
            Target t = new Target("t1", label, "🐱", 0.5, 0.5, 0.05);
            assertRejected("p_label", List.of(picture("p_label", 0.5, t)));
        }
    }

    @Test
    void blankEmojiRejected() {
        for (String emoji : new String[] {"", "  ", null}) {
            Target t = new Target("t1", "고양이", emoji, 0.5, 0.5, 0.05);
            assertRejected("p_emoji", List.of(picture("p_emoji", 0.5, t)));
        }
    }

    @Test
    void missingRadiusKeyInJsonIsRejected() throws Exception {
        String json = "[{\"pictureId\":\"p_json\",\"mode\":\"OBJECT\",\"image\":\"a.png\",\"aspectRatio\":0.5,"
                + "\"targets\":[{\"targetId\":\"t1\",\"label\":\"고양이\",\"emoji\":\"🐱\",\"x\":0.5,\"y\":0.5}]}]";
        List<Picture> pictures = new ObjectMapper().readValue(json,
                new com.fasterxml.jackson.core.type.TypeReference<List<Picture>>() {});
        assertRejected("p_json", pictures);
    }

    @Test
    void duplicateTargetIdInPictureRejected() {
        assertRejected("p_dup_t", List.of(picture("p_dup_t", 0.5, target("t1", 0.1, 0.1, 0.05), target("t1", 0.2, 0.2, 0.05))));
    }

    @Test
    void duplicatePictureIdRejected() {
        assertRejected("p_same", List.of(valid("p_same"), valid("p_same")));
    }

    @Test
    void zeroAspectRatioRejected() {
        assertRejected("p_ar", List.of(picture("p_ar", 0, target("t1", 0.5, 0.5, 0.05))));
    }

    @Test
    void sameTargetIdInDifferentPicturesIsAllowed() {
        assertDoesNotThrow(() -> PictureCatalog.validate(List.of(valid("p1"), valid("p2"))));
    }

    @Test
    void actualCatalogJsonIsValid() {
        PictureCatalog catalog = new PictureCatalog(PictureCatalog.read(new ObjectMapper()));
        assertFalse(catalog.findByMode(Mode.OBJECT).isEmpty());
    }
}
