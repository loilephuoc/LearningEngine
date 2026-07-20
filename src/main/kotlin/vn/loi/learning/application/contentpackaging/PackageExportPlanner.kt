package vn.loi.learning.application.contentpackaging

/**
 * Chuẩn bị toàn bộ kế hoạch export trước khi chuyển cho Infrastructure.
 *
 * Ở giai đoạn hiện tại planner chỉ tạo manifest.json.
 * Các bước tiếp theo sẽ lần lượt bổ sung metadata.json, contents.json
 * và learning-items.json vào cùng PackageExportPlan.
 */
class PackageExportPlanner(
    private val planFactory: PackageExportPlanFactory = PackageExportPlanFactory()
) {

    fun plan(
        payload: PackageExportPayload
    ): PackageExportPlan =
        planFactory.create(payload)
}
