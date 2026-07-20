package vn.loi.learning.application.contentpackaging

class NumericPackageVersion private constructor(
    components: List<Int>
) : Comparable<NumericPackageVersion> {

    private val components =
        normalize(
            components
        )

    init {
        require(components.isNotEmpty()) {
            "Package version must contain at least one component."
        }

        require(
            components.all { component ->
                component >= 0
            }
        ) {
            "Package version components must not be negative."
        }
    }

    override fun compareTo(
        other: NumericPackageVersion
    ): Int {
        val componentCount =
            maxOf(
                components.size,
                other.components.size
            )

        for (index in 0 until componentCount) {
            val currentComponent =
                components.getOrElse(index) {
                    0
                }

            val otherComponent =
                other.components.getOrElse(index) {
                    0
                }

            if (currentComponent != otherComponent) {
                return currentComponent.compareTo(
                    otherComponent
                )
            }
        }

        return 0
    }

    override fun equals(
        other: Any?
    ): Boolean =
        this === other ||
                (
                        other is NumericPackageVersion &&
                                components == other.components
                        )

    override fun hashCode(): Int =
        components.hashCode()

    override fun toString(): String =
        components.joinToString(
            separator = "."
        )

    companion object {

        fun parseOrNull(
            value: String
        ): NumericPackageVersion? {
            val parts =
                value.split(".")

            if (
                parts.isEmpty() ||
                parts.any { part ->
                    part.isEmpty() ||
                            !part.all { character ->
                                character.isDigit()
                            }
                }
            ) {
                return null
            }

            val components =
                parts.map { part ->
                    part.toIntOrNull()
                        ?: return null
                }

            return NumericPackageVersion(
                components
            )
        }

        fun parse(
            value: String
        ): NumericPackageVersion =
            parseOrNull(
                value
            ) ?: throw InvalidPackageVersionException(
                value
            )

        private fun normalize(
            components: List<Int>
        ): List<Int> {
            val lastNonZeroIndex =
                components.indexOfLast { component ->
                    component != 0
                }

            return if (lastNonZeroIndex < 0) {
                listOf(
                    0
                )
            } else {
                components.take(
                    lastNonZeroIndex + 1
                )
            }
        }
    }
}