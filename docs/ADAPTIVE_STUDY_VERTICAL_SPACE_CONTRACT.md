# Adaptive Study Vertical Space Contract

`StudyVerticalSpaceAllocationResolver` reserves only required fixed content, the true outer typing
minimum, Decision Dock, system controls, safe spacing, visible inventory, and expanded examples.
The remaining viewport height is the image budget; surplus height increases the image instead of
inflating the typing card or creating an empty gap.

`CenteredTypingField` keeps two separate constraints. Its inner line-box minimum protects glyph
leading, descenders, cursor, placeholder, and selection. Its outer minimum covers the label,
single-line content, trailing action, and padding without the former worst-case reserve. Typography
is unchanged.

Wide, standard landscape, square, portrait, and extreme portrait images receive the real remaining
height rather than an arbitrary aspect-class shrink factor. `ContentScale.Fit` preserves intrinsic
aspect ratio without crop or stretch. Constrained layouts reduce spacing and image first, preserve
typing and dock minima, and use bounded scrolling only when minimum content cannot fit. Rating
Inventory remains a headerless, non-collapsible six-item semantic surface. Integrated Desktop UAT
remains pending.
