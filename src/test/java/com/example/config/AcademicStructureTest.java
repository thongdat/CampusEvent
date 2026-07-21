package com.example.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho ánh xạ ngành -> khoa và chuẩn hóa tên (bỏ dấu, viết tắt).
 *
 * Ví dụ Equivalence Partitioning: chia dữ liệu thành các nhóm tương đương
 * (đúng khoa / ngành thuộc khoa / tên viết tắt / tên không tồn tại) và test đại diện.
 */
@DisplayName("Cấu trúc học thuật - Ánh xạ ngành/khoa")
class AcademicStructureTest {

    @Test
    @DisplayName("Ngành thuộc khoa -> tìm đúng khoa cha")
    void majorMapsToFaculty() {
        assertEquals("Công nghệ Thông tin", AcademicStructure.facultyOf("Kỹ thuật phần mềm"));
        assertEquals("Kinh tế", AcademicStructure.facultyOf("Marketing"));
    }

    @Test
    @DisplayName("Tên viết tắt / tiếng Anh -> nhận diện đúng khoa")
    void aliasMapsToFaculty() {
        assertEquals("Công nghệ Thông tin", AcademicStructure.facultyOf("CNTT"));
        assertEquals("Công nghệ Thông tin", AcademicStructure.facultyOf("Information Technology"));
        assertEquals("Kinh tế", AcademicStructure.facultyOf("Economics"));
    }

    @Test
    @DisplayName("Không phân biệt dấu tiếng Việt và hoa/thường")
    void normalizationIgnoresDiacriticsAndCase() {
        assertEquals("Công nghệ Thông tin", AcademicStructure.canonicalDepartment("cong nghe thong tin"));
        assertTrue(AcademicStructure.isFaculty("kinh te"));
    }

    @Test
    @DisplayName("Tên không tồn tại -> trả về 'Khác'")
    void unknownReturnsOther() {
        assertEquals("Khác", AcademicStructure.facultyOf("Ngành Không Có Thật"));
        assertFalse(AcademicStructure.isKnownDepartment("Ngành Không Có Thật"));
    }

    @Test
    @DisplayName("Kiểm tra ngành có thuộc khoa hay không")
    void belongsToFaculty() {
        assertTrue(AcademicStructure.belongsToFaculty("Công nghệ Thông tin", "An toàn thông tin"));
        assertFalse(AcademicStructure.belongsToFaculty("Kinh tế", "An toàn thông tin"));
    }
}
