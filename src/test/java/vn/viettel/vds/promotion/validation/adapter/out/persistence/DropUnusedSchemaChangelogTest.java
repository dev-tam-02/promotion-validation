package vn.viettel.vds.promotion.validation.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Changelog 088 (bỏ cột cũ không dùng) và 089 (bỏ bảng publish_jobs). Đã chạy thử changelog thật trên schema tạm. */
@DisplayName("Changelog 088, 089 — bỏ cột, bảng không dùng")
class DropUnusedSchemaChangelogTest {

    private static String read(String path) throws Exception {
        try (InputStream in = DropUnusedSchemaChangelogTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void dropsUnusedColumnsAndPublishJobs() throws Exception {
        assertThat(read("db/changelog/changes/088-drop-unused-columns.yaml"))
                .contains("ALTER TABLE operator_categories DROP COLUMN IF EXISTS display_name")
                .contains("ALTER TABLE reason_codes DROP COLUMN IF EXISTS display_message")
                .contains("DROP INDEX IF EXISTS idx_rb_resource_active ON rule_bindings_deleted")
                .contains("DROP COLUMN IF EXISTS traffic_control")
                .contains("ALTER TABLE outbox_events DROP COLUMN IF EXISTS tenant_id");
        assertThat(read("db/changelog/changes/089-drop-publish-jobs.yaml")).contains("DROP TABLE IF EXISTS publish_jobs");

        String master = read("db/changelog/db.changelog-master.yaml");
        int i87 = master.indexOf("087-campaign-archive-rule-bindings-deleted.yaml");
        int i88 = master.indexOf("088-drop-unused-columns.yaml");
        int i89 = master.indexOf("089-drop-publish-jobs.yaml");
        assertThat(i87).isPositive();
        assertThat(i88).isGreaterThan(i87);
        assertThat(i89).isGreaterThan(i88);
    }
}
