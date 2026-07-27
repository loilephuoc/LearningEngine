package vn.loi.learning.desktop.runtime

import vn.loi.learning.domain.study.session.model.SessionPolicy

fun DesktopRuntimeConfiguration.toSessionPolicy(): SessionPolicy =
    SessionPolicy(
        newItemLimit = newItemsPerSession,
        reviewItemLimit = reviewItemsPerSession
    )
