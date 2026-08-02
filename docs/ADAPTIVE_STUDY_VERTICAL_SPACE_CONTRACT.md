# Adaptive Study Vertical Space Contract

`StudyVerticalSpaceAllocationResolver` centrally reserves translation/POS, timer, the complete
`CenteredTypingField`, Decision Dock, safe spacing, visible inventory, expanded examples, and
typography scale before allocating image space.

Image aspect is typed as wide landscape, standard landscape, square, portrait, or extreme.
Portrait and extreme portrait reduce maximum height and width before any typing allocation can be
lost. Images retain `ContentScale.Fit`, intrinsic aspect ratio, and may leave horizontal whitespace;
they are never cropped or stretched. Short layouts reduce spacing and image height first, then
request bounded scrolling only when reserved minimum content itself exceeds the viewport.

When typing is required, the resolved minimum field height and trailing action remain allocated and
the Decision Dock remains reserved. Inventory renders six direct semantic items without a visual
header or collapse row. Integrated portrait/extreme-portrait Desktop UAT remains pending.
