# Adaptive Study Vertical Space Contract

`TypingFieldLayoutMetricsResolver` derives the outer typing minimum from label height, the protected
inner line box, top/bottom insets, label gap, and trailing-action diameter. It is content-derived,
not a fixed viewport-class hero height, and grows only when the input genuinely becomes multiline.

`StudyVerticalSpaceAllocationResolver` reserves only required body content and measured spacing.
The owning viewport excludes header, Decision Dock, footer, and safe insets before calling it; any
remaining external surface such as visible inventory is supplied once as typed
`externalReservedHeightDp`, never inferred or subtracted twice inside the image block.
The remaining viewport height is the image budget; surplus height increases the image instead of
inflating the typing card or creating an empty gap.

`CenteredTypingField` keeps two separate constraints. Its inner line-box minimum protects glyph
leading, descenders, cursor, placeholder, and selection. Its outer minimum covers the label,
single-line content, trailing action, and padding without the former worst-case reserve. Typography
is unchanged.

Wide, standard landscape, square, portrait, and extreme portrait images receive the complete real
remaining height rather than an arbitrary aspect-class fraction or legacy layout height cap.
`ContentScale.Fit` preserves intrinsic
aspect ratio without crop or stretch. Constrained layouts reduce spacing and image first, preserve
typing and dock minima, and use bounded scrolling only when minimum content cannot fit. Rating
Inventory remains a headerless, non-collapsible six-item semantic surface. Integrated Desktop UAT
remains pending.
