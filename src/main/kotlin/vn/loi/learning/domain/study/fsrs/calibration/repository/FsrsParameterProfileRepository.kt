package vn.loi.learning.domain.study.fsrs.calibration.repository

import vn.loi.learning.domain.study.fsrs.calibration.model.FsrsParameterProfile
import vn.loi.learning.domain.study.fsrs.calibration.model.FsrsParameterProfileId

/**
 * Repository contract quản lý FSRS Parameter Profile.
 *
 * Domain chỉ định nghĩa contract.
 * Infrastructure sẽ cung cấp implementation cụ thể.
 */
interface FsrsParameterProfileRepository {

    /**
     * Lưu mới hoặc cập nhật một profile.
     */
    fun save(
        profile: FsrsParameterProfile
    )

    /**
     * Tìm profile theo định danh.
     */
    fun findById(
        id: FsrsParameterProfileId
    ): FsrsParameterProfile?

    /**
     * Trả về toàn bộ profile hiện có.
     */
    fun findAll(): List<FsrsParameterProfile>

    /**
     * Kiểm tra profile có tồn tại hay không.
     */
    fun exists(
        id: FsrsParameterProfileId
    ): Boolean =
        findById(id) != null
}