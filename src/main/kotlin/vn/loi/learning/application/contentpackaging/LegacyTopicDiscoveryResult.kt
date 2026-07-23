package vn.loi.learning.application.contentpackaging

data class LegacyTopicDiscoveryResult(
    val pairs: List<ValidatedLegacyTopicPair>,
    val diagnostics: List<LegacyTopicDiscoveryDiagnostic>
) {

    val isValid: Boolean
        get() =
            diagnostics.isEmpty()
}
