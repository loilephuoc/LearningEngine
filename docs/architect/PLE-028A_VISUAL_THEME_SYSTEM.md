# Learning Engine 2.0 — Visual Theme System Foundation (PLE-028A & PLE-028A.1)

**Document Status:** Approved Architectural Specification & Design Constitution (Source of Truth for PLE-028+)  
**Author:** Principal Product Designer & Design System Architect  
**Capability:** `PLE-028A — Visual Theme System Foundation` & `PLE-028A.1 — Design Language Completion`  
**Target Scope:** Design Language, Semantic Theme Tokens, Accessibility, Surface Elevation, Typography, Spacing, Shape, Motion, Iconography, Component States, Focus, Density, Image System, Theme Independence Contract & Governance  

---

## 1. DESIGN PRINCIPLES & RATIONALE

Learning Engine 2.0 được thiết kế cho việc học tập sâu (Deep Learning) và học tập kéo dài hàng nghìn giờ. Đây không phải là ứng dụng demo, không phải Material sample hay Compose sample. Mọi nguyên lý thiết kế đều lấy trải nghiệm nhận thức và sự thoải mái thị giác lâu dài của người học làm gốc.

### 1.1. Learning First (Học tập làm gốc)
* **Rationale:** Giao diện không tồn tại để khoe sắc hay trang trí cầu kỳ. Mọi chi tiết đồ họa (pixel, đường viền, khoảng trắng, bóng đổ) đều phải trả lời câu hỏi: *"Chi tiết này có giúp người học ghi nhớ tốt hơn hay làm giảm mệt mỏi thị giác không?"*
* **Quy tắc:** Loại bỏ hoàn toàn hiệu ứng chuyển động rườm rà, bóng đổ gắt, màu sắc phản quang gây xao nhãng khỏi khu vực học tập trung.

### 1.2. Content First (Nội dung là trung tâm)
* **Rationale:** Từ vựng, ngữ nghĩa, câu ví dụ, phát âm và phương tiện học tập là ngôi sao chính của màn hình. Thẻ chứa (Card container), thanh công cụ (Toolbar), đường phân cách (Divider) và nền cửa sổ (Window background) phải lùi sâu về phía sau để làm nổi bật nội dung.
* **Quy tắc:** Thẻ chứa không sử dụng viền quá đậm hoặc nền tương phản mạnh với chữ. Nội dung học tập luôn có kích thước font lớn nhất, độ tương phản cao nhất và vị trí ưu tiên nhất.

### 1.3. Low Visual Noise (Cực kỳ tiết chế nhiễu thị giác)
* **Rationale:** Sự mệt mỏi thị giác (visual fatigue) khi sử dụng ứng dụng liên tục trong 2–4 tiếng/ngày thường bắt nguồn từ "nhiễu thị giác" — quá nhiều viền (borders), nhiều mảng màu background khác nhau, bóng đổ đè lên nhau, và các icon rải rác.
* **Quy tắc:** Ưu tiên dùng khoảng trắng (whitespace) và phân cấp kiểu chữ (typographic hierarchy) thay cho đường kẻ ngang/dọc. Chỉ dùng tối đa 1-2 điểm nhấn màu (accent spot) trên một góc nhìn.

### 1.4. Long Reading Comfort (Thoải mái đọc trong thời gian dài)
* **Rationale:** Nền ngả tím/lavender nhẹ (`#F8FAFC` ngả pha tím nhạt) ở Light Theme trước đây tạo cảm giác không thật sự sạch sẽ, gây nhòe mắt khi đọc chữ đen kéo dài. Template Anki gốc thành công vì sử dụng nền trắng phẳng tinh khiết (`#FFFFFF` hoặc `#FAFAFA`), đọc cực kỳ thoải mái và tự nhiên như đọc sách giấy.
* **Quy tắc:** Light Theme chuyển sang nền sạch tuyệt đối, phẳng, không có sắc xám/tím dơ. Dark Theme chuyển sang tông xám đen trầm tĩnh, ấm áp, hoàn toàn loại bỏ hiện tượng "chói mắt" (glare) và lỗi mờ nhòe chữ (gray soup).

### 1.5. Minimal Accent System (Hệ thống màu nhấn tiết chế)
* **Rationale:** Màu nhấn (Accent) nếu xuất hiện tràn lan trên màn hình sẽ mất đi giá trị thu hút sự chú ý.
* **Quy tắc:** Màu Accent (Primary Purple/Indigo) chỉ được xuất hiện ở 3 vị trí: (1) Trạng thái Focus của bàn phím, (2) Nút hành động chính duy nhất (Primary CTA / Space action), và (3) Chỉ báo tiến trình active (Active Progress).

### 1.6. Accessibility & Contrast Integrity (Tính tiếp cận & Độ tương phản chuẩn mực)
* **Rationale:** Đã xảy ra hiện tượng mờ chữ IPA, mờ ví dụ, mờ Pause text ở Dark Theme do dùng alpha/gray thiếu kiểm soát.
* **Quy tắc:** Mọi văn bản chính phải đạt chuẩn **WCAG 2.1 AAA** (tương phản $\ge 7:1$). Các văn bản phụ, nhãn metadata phải đạt chuẩn **WCAG 2.1 AA** (tương phản $\ge 4.5:1$). Không bao giờ dùng độ mờ `alpha < 0.6f` cho văn bản có nghĩa.

### 1.7. Desktop-First Density & Spatial Ergonomics (Trải nghiệm Desktop chuyên nghiệp)
* **Rationale:** Ứng dụng Desktop chạy trên màn hình lớn với bàn phím và chuột. Không thể mang tư duy padding to bản của Mobile lên Desktop.
* **Quy tắc:** Mật độ thông tin vừa phải, hỗ trợ phím tắt rõ ràng (Keybinding badges), hỗ trợ hover feedback tinh tế, hiển thị trạng thái focus rõ nét khi duyệt phím Tab/Space/1-4.

---

## 2. MOODBOARD & DESIGN IDENTITY

Learning Engine sở hữu định hình thẩm mỹ được truyền cảm hứng từ những ứng dụng năng suất hàng đầu thế giới: **Linear, Notion, Readwise, Raycast, Arc Browser, Superhuman** và **Apple Human Interface Guidelines (HIG)**.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                          LEARNING ENGINE                               │
│                      Design System Identity                            │
├───────────────────┬───────────────────┬────────────────────────────────┤
│    ACADEMIC       │   PROFESSIONAL    │           FOCUSED              │
│ Sâu sắc, tri thức │ Chuẩn mực, tinh tế│ Zero phế liệu, tĩnh lặng tuyệt đối│
├───────────────────┼───────────────────┼────────────────────────────────┤
│      CALM         │     PREMIUM       │      MODERN MINIMAL            │
│  Thư thái mắt     │Đậm chất Apple/Linear│ Rõ ràng, nhịp điệu typographic │
└───────────────────┴───────────────────┴────────────────────────────────┘
```

* **Academic & Scholarly:** Cảm giác như một thư viện cao cấp hay một cuốn sách ghi chép học thuật hiện đại. Chữ nghĩa sắc nét, phân cấp bố cục logic.
* **Professional & Precision:** Mọi chi tiết đều có mục đích. Viền mỏng $1\text{dp}$ cực kỳ tinh tế, bo góc cân đối ($8\text{dp}$ đến $12\text{dp}$), khoảng cách chuẩn mực ($4\text{dp}, 8\text{dp}, 12\text{dp}, 16\text{dp}, 24\text{dp}$).
* **Focused & Calm:** Môi trường học tập không tiếng ồn. Giúp não bộ đi vào trạng thái **Flow State** nhanh nhất và ở lại lâu nhất.
* **Premium Modern Minimal:** Loại bỏ các gradient sặc sỡ, loại bỏ Material 3 Ripple quá đậm đà, sử dụng tương tác hover/press phản hồi mềm mại, nhanh gọn ($100\text{ms} - 150\text{ms}$).

---

## 3. LIGHT THEME ARCHITECTURE & SURFACE HIERARCHY

### 3.1. Khắc phục sự cố Light Theme hiện tại
* **Vấn đề đã xác nhận:** Nền ngả lavender/pha xám tím (`#F8FAFC`, `#F5F3FF`, `#EDE9FE`) làm màn hình bị đục, không tạo cảm giác sạch sẽ tuyệt đối. Template Anki dùng nền trắng giúp đọc thoải mái hơn nhiều.
* **Giải pháp:** Thiết lập lại hệ thống Surface Elevation dựa trên nền trắng giấy sạch chuẩn mực (`Pure Crisp Canvas`).

### 3.2. Light Theme Surface & Color Tokens Specification

| Token Name | Conceptual Role | Target Visual Value | Usage Guidelines & Rationale |
| :--- | :--- | :--- | :--- |
| `windowBackground` | Canvas toàn ứng dụng | `#FAFAFA` (Clean Slate 50) | Nền trung tính sạch tuyệt đối, dịu mắt, loại bỏ sắc lavender. |
| `surfacePrimary` | Nền thẻ chính (Card background) | `#FFFFFF` (Pure White) | Thẻ học tập, thẻ chứa từ vựng chính. Tách biệt với Canvas bằng viền siêu nhẹ. |
| `surfaceSecondary` | Thẻ phụ / Nhóm phụ | `#F4F4F5` (Zinc 100) | Khu vực chứa IPA/POS, ô nhập liệu Typing, vùng thông tin phụ. |
| `surfaceMeaning` | Thẻ Giải nghĩa (Meaning Card) | `#F8FAF6` hoặc `#F0FDF4` nhạt | Nền thẻ nghĩa tiếng Việt. Tông xanh lá nhạt siêu dịu, đại diện cho sự thấu hiểu. |
| `surfaceExample` | Thẻ Câu ví dụ (Example Card) | `#F8FAFC` (Slate 50 nhạt) | Nền chứa ví dụ câu song ngữ. Phẳng, trung tính, đọc rõ ràng. |
| `surfaceScheduler` | Nền dock đánh giá / Feedback | `#FAFAFA` | Thanh điều khiển FSRS & phím rating. Giữ nguyên độ phẳng trung tính. |
| `surfaceToolbar` | Nền thanh tiêu đề / Header | `#FFFFFF` | Header cửa sổ và thông tin tiến trình bài học. |
| `borderSubtle` | Đường viền siêu nhẹ | `#E4E4E7` (Zinc 200) | Định hình thẻ chứa mà không gây nhiễu mắt. Rộng $1\text{dp}$. |
| `borderMedium` | Đường viền vừa | `#D4D4D8` (Zinc 300) | Viền ô nhập liệu, viền thẻ active. |
| `borderFocus` | Viền tập trung phím/bàn phím | `#6D28D9` / `#7C3AED` | Highlight $1.5\text{dp} - 2\text{dp}$ khi duyệt bằng bàn phím. |
| `textPrimary` | Văn bản chính | `#09090B` (Zinc 950) | Chữ từ vựng, nghĩa chính, câu tiếng Anh. Tương phản $> 15:1$. |
| `textSecondary` | Văn bản phụ | `#52525B` (Zinc 600) | Dịch câu ví dụ, nhãn thông tin. Tương phản $> 7:1$. |
| `textMuted` | Văn bản nhạt / Metadata | `#71717A` (Zinc 500) | Ký tự IPA, phím tắt hint, timestamp. Tương phản $> 4.5:1$. |
| `textDisabled` | Văn bản vô hiệu hóa | `#A1A1AA` (Zinc 400) | Trạng thái không khả dụng. Tương phản $> 3:1$. |
| `accentPrimary` | Màu nhấn chính | `#6D28D9` (Violet 700) | Nút xem đáp án, nút chính, trạng thái active. |
| `accentHover` | Màu nhấn di chuột | `#5B21B6` (Violet 800) | Feedback hover cho nút chính. |
| `accentSoft` | Nền nhấn nhẹ | `#F3E8FF` (Purple 100) | Highlight ô đang được chọn hoặc badge active. |
| `danger` | Cảnh báo / Nút Again | `#DC2626` (Red 600) | Đánh giá Again, câu sai, lỗi hệ thống. |
| `warning` | Thận trọng / Nút Hard | `#D97706` (Amber 600) | Đánh giá Hard, cảnh báo trùng lặp. |
| `success` | Thành công / Nút Good/Easy | `#16A34A` (Green 600) | Đánh giá Good/Easy, bài học hoàn thành. |

---

## 4. DARK THEME ARCHITECTURE & CONTRAST REMEDIATION

### 4.1. Khắc phục sự cố Dark Theme hiện tại
* **Vấn đề đã xác nhận:**
  1. Ký tự IPA quá mờ (mất hút trên nền tối).
  2. Nghĩa từ vựng quá mờ, không tách biệt được thông tin chính/phụ.
  3. Câu ví dụ gần như biến mất do dùng độ mờ `alpha` quá thấp.
  4. Thanh tiến trình (Progress bar) mờ nhạt.
  5. Nút Pause text gần như không đọc được.
  6. Disabled states không phân biệt được với nền.
  7. Xảy ra hiện tượng "gray soup" (tất cả các thẻ mờ xám giống nhau, không có độ sâu thị giác).

### 4.2. Nguyên lý xây dựng Dark Theme chuẩn mực (Linear/Raycast Standard)
* **Zero Gray Soup via Luminance Layering:** Phân tách rõ ràng 4 tầng độ sáng (Luminance Levels):
  - Layer 0 (Window Background): Deep Charcoal `#090D16` / `#0C0A09` (gần đen nhưng không phải `#000000` tuyệt đối để tránh ô-lét chói mắt).
  - Layer 1 (Surface/Card): Crisp Slate-Zinc `#161B26` / `#18181B`.
  - Layer 2 (Elevated Surface): Medium Slate `#212838` / `#27272A`.
  - Layer 3 (Overlay / Modal / Focus): Bright Slate `#2D3748` / `#3F3F46`.
* **High Contrast Typography Enforcement:** Mọi text trong Dark Theme sử dụng tông xám sáng/trắng ấm (`#F8FAFC`, `#F1F5F9`, `#E2E8F0`), loại bỏ hoàn toàn việc opacity hóa text dưới 0.7f.

### 4.3. Dark Theme Surface & Color Tokens Specification

| Token Name | Conceptual Role | Target Visual Value | Contrast & Remediation Target |
| :--- | :--- | :--- | :--- |
| `windowBackground` | Canvas toàn ứng dụng | `#090D16` (Deep Charcoal Black) | Đáy không gian tối, đầm mắt, zero chói sáng. |
| `surfacePrimary` | Nền thẻ chính (Card background) | `#161B26` (Dark Slate Zinc) | Thẻ học chính. Nổi lên 1 lớp so với Canvas. |
| `surfaceSecondary` | Thẻ phụ / Nhóm phụ | `#212838` (Elevated Slate) | Chứa IPA/POS, ô nhập liệu Typing. |
| `surfaceMeaning` | Thẻ Giải nghĩa (Meaning Card) | `#062E1B` hoặc `#0F291E` | Nền nghĩa tiếng Việt tông xanh mờ trầm sâu. |
| `surfaceExample` | Thẻ Câu ví dụ (Example Card) | `#121824` (Subtle Slate) | Nền ví dụ câu song ngữ tối dịu. |
| `surfaceScheduler` | Nền dock đánh giá / Feedback | `#161B26` | Thanh điều khiển FSRS phẳng, trùng màu thẻ chính. |
| `surfaceToolbar` | Nền thanh tiêu đề / Header | `#161B26` | Header phẳng đồng bộ. |
| `borderSubtle` | Đường viền siêu nhẹ | `#273248` (Slate Border) | Phân tách thẻ sắc nét trong bóng tối ($1\text{dp}$). |
| `borderMedium` | Đường viền vừa | `#3B4A6B` | Viền ô nhập liệu / hover state. |
| `borderFocus` | Viền tập trung phím | `#A78BFA` (Vivid Purple) | Viền sáng nổi bật khi focus phím tắt ($2\text{dp}$). |
| `textPrimary` | Văn bản chính | `#F8FAFC` (Bright Off-White) | **Fix UAT:** Từ vựng & nghĩa chính sáng rõ (Tương phản $> 14:1$). |
| `textSecondary` | Văn bản phụ | `#E2E8F0` (Light Slate) | **Fix UAT:** Nghĩa Tiếng Việt / Dịch câu rõ nét (Tương phản $> 9:1$). |
| `textMuted` | Metadata / IPA / Hint | `#94A3B8` (Medium Slate) | **Fix UAT:** IPA & Example EN hoàn toàn đọc rõ (Tương phản $> 6:1$). |
| `textDisabled` | Văn bản vô hiệu hóa | `#64748B` (Muted Zinc) | **Fix UAT:** Phân biệt rõ ràng với nền (Tương phản $> 3.5:1$). |
| `accentPrimary` | Màu nhấn chính | `#8B5CF6` (Bright Violet) | Nút bấm & điểm nhấn nổi bật trên nền tối. |
| `accentHover` | Màu nhấn di chuột | `#A78BFA` | Feedback di chuột rực rỡ nhẹ. |
| `accentSoft` | Nền nhấn nhẹ | `#2E1065` (Deep Purple Tone) | Highlight mảng được chọn. |
| `danger` | Cảnh báo / Nút Again | `#EF4444` (Vivid Red) | Sáng rõ, dễ nhận biết. |
| `warning` | Thận trọng / Nút Hard | `#F59E0B` (Vivid Amber) | Sáng rõ, ấm áp. |
| `success` | Thành công / Nút Good/Easy | `#22C55E` (Vivid Green) | Sáng rõ, dễ nhận biết. |

---

## 5. TYPOGRAPHY TOKENS & ROLE HIERARCHY

Hệ thống Typography không chỉ định nghĩa font-size mà định nghĩa **Vai trò truyền tải nhận thức (Cognitive Communication Role)**.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        TYPOGRAPHY HIERARCHY                            │
├────────────────────────────────────────────────────────────────────────┤
│ DISPLAY          Words / Headings (Focal Point 1: 28-36sp, Bold)       │
│ HEADLINE         Pane / Card Titles (Focal Point 2: 18-22sp, SemiBold)   │
│ MEANING          Vietnamese Translations (Focal Point 3: 16-18sp, Bold)  │
│ BODY             English Definitions / Passages (14-16sp, Regular/Medium)│
│ EXAMPLE          Bilingual Usage Sentences (14-16sp, Distinct Weights)   │
│ METADATA         IPA / POS Status / Tags (12-13sp, Monospace/Italic)    │
│ SCHEDULER        FSRS Intervals & Ratings (11-12sp, Medium)            │
│ SHORTCUT BADGE   Keyboard Binding Hints (10-11sp, SemiBold Monospace)     │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 6. COMPONENT SURFACE MAP

Bảng Mapping quy định cụ thể việc áp dụng các Token Surface, Border và Typography vào từng Màn hình & Component trong Learning Engine.

---

## 7. ACCESSIBILITY & INTERACTION SYSTEM

---

## 8. SEMANTIC COLOR SYSTEM

---

## 9. VISUAL HIERARCHY RULES

---

## 10. IMPLEMENTATION ROADMAP & SUB-CAPABILITIES

---

## 11. SPACING SYSTEM

### 11.1. Objective
Thiết lập hệ thống **Geometric 4dp/8dp Spacing Scale** hoàn chỉnh cho toàn bộ ứng dụng Desktop, thay thế hoàn toàn các giá trị pixel/dp tự do (hardcoded magic numbers).

### 11.2. Rationale
Khoảng cách thị giác tạo nên nhịp điệu đọc (Vertical & Horizontal Rhythm). Việc sử dụng các khoảng cách không đồng nhất làm tăng nhận thức thị giác thừa (Cognitive Load). Một Spacing Scale chuẩn mực 8-point grid giúp duy trì sự cân đối chuẩn xác giữa các UI components trên Desktop màn hình rộng.

### 11.3. Design Decisions & Spacing Scale Tokens

| Token Name | Value (dp) | Semantic Role & Usage Guidelines |
| :--- | :--- | :--- |
| `space0` | `0dp` | Zero spacing / Reset margin. |
| `space1` | `2dp` | **Micro spacing:** Border offset, focus outline gap, micro badge padding. |
| `space2` | `4dp` | **Tight spacing:** Khoảng cách giữa icon và label, khoảng cách giữa badge và title. |
| `space3` | `8dp` | **Inline spacing:** Khoảng cách giữa các nút bấm nội dòng, padding trong ô nhập liệu compact. |
| `space4` | `12dp` | **Component spacing:** Padding nội bộ của Card nhỏ, khoảng cách giữa các trường dữ liệu phụ. |
| `space5` | `16dp` | **Standard Container spacing:** Padding chuẩn của Study Card, Content Block, List Item. |
| `space6` | `24dp` | **Section spacing:** Khoảng cách giữa Word Area và Meaning Card, giữa các khối nội dung lớn. |
| `space7` | `32dp` | **Large Section spacing:** Khoảng cách giữa Header và Workspace Body. |
| `space8` | `48dp` | **Page Gutter / Margin:** Lề ngoài màn hình Desktop ở chế độ Standard Viewport. |
| `space9` | `64dp` | **Wide Page Gutter:** Lề ngoài màn hình ở chế độ Wide Viewport ($\ge 1024\text{dp}$). |

### 11.4. Vertical Rhythm & Layout Rules
* **Vertical Rhythm:** Mọi chiều cao dòng (Line-height) và margin dọc giữa các đoạn văn bản phải được căn khớp theo bội số của $4\text{dp}$.
* **Desktop Gutter Contract:** Màn hình Study Screen tự động điều chỉnh Gutter ngang theo Viewport:
  - `COMPACT` ($< 600\text{dp}$): Horizontal Gutter = `space4` ($12\text{dp}$).
  - `STANDARD` ($600 - 1023\text{dp}$): Horizontal Gutter = `space6` ($24\text{dp}$).
  - `WIDE` ($\ge 1024\text{dp}$): Horizontal Gutter = `space9` ($64\text{dp}$) với Content Box căn giữa cố định $800\text{dp}$.

### 11.5. Future Implementation Guidance
Tất cả Composable layouts bắt buộc phải sử dụng `LESpacing.spaceX` từ file Design System tokens, nghiêm cấm sử dụng các hằng số dp tự do như `13.dp`, `17.dp` trong UI code.

---

## 12. SHAPE SYSTEM

### 12.1. Objective
Quy định thống nhất hệ thống bo góc **Corner Radius Scale** cho toàn bộ các thành phần giao diện từ micro control (Badge, Tooltip) đến macro container (Card, Modal Dialog).

### 12.2. Rationale
Bo góc (Border Radius) thể hiện mức độ mềm mại và tính bao bọc của phần tử UI. Góc bo nhất quán tạo ra ngôn ngữ thiết kế đồng bộ, ngăn ngừa hiện tượng "xung đột độ cong" (Radius mismatch) khi các container nằm lồng vào nhau.

### 12.3. Design Decisions & Shape Scale Tokens

| Token Name | Value (dp) | Target Components & Usage Guidelines |
| :--- | :--- | :--- |
| `radiusNone` | `0dp` | Cạnh sắc tuyệt đối: Window boundary, Divider kẻ ngang/dọc. |
| `radiusXS` | `4dp` | **Micro controls:** Focus outline, Tooltip, Shortcut keybinding badge, Checkbox. |
| `radiusS` | `6dp` | **Compact controls:** Dropdown menu, Sub-menu item, Compact Tag, POS status badge. |
| `radiusM` | `8dp` | **Standard controls:** Base Button, Input Text Field, Audio playback control button, IPA container. |
| `radiusL` | `12dp` | **Primary Containers:** Meaning Card, Example Card, Rating Dock Container, Settings Section Card. |
| `radiusXL` | `16dp` | **Major Surface Containers:** Primary Study Workspace Card, Content Studio Hero Viewport. |
| `radius2XL` | `24dp` | **Overlay Modals:** Floating Action Sheet, Major Dialog Container. |
| `radiusPill` | `999dp` | **Pill Shape:** Progress Bar track/indicator, Active Filter Chip, Status Indicator Badge. |
| `radiusCircle`| `50%` | **Circular:** Round Icon Button, Avatar image, Play/Pause circular audio control. |

### 12.4. Nested Radius Formula (Quy tắc lồng Shape)
Để đảm bảo các đường cong song song tuyệt đối khi lồng thẻ nhỏ vào thẻ lớn:
$$R_{\text{inner}} = R_{\text{outer}} - P_{\text{padding}}$$
*Ví dụ:* Nếu Study Workspace Outer Card có $R_{\text{outer}} = 16\text{dp}$ và Padding $P = 8\text{dp}$, thì thẻ bên trong (Inner Card) bắt buộc phải sử dụng $R_{\text{inner}} = 8\text{dp}$ (`radiusM`).

### 12.5. Future Implementation Guidance
Tất cả `Shape` trong Compose UI phải sử dụng `LEShape.S`, `LEShape.M`, `LEShape.L`, v.v. Nghiêm cấm dùng `RoundedCornerShape(7.dp)`.

---

## 13. ELEVATION & LAYERING SYSTEM

### 13.1. Objective
Xây dựng kiến trúc phân tầng giao diện (**Visual Layering & Z-Index Elevation System**) kết hợp giữa Độ sáng bề mặt (Luminance) và Bóng đổ dịu (Subtle Shadow) phù hợp cho cả Light & Dark Theme.

### 13.2. Rationale
Trên môi trường Desktop, độ sâu thị giác (Depth perception) giúp người dùng nhận biết tức thì phần tử nào đang ở trên (Active/Overlay) và phần tử nào đang ở dưới (Background). Dark Theme không thể dựa vào bóng đổ màu đen (do nền đã tối), vì vậy độ sâu phải được thiết lập bằng tầng độ sáng bề mặt (Luminance elevation).

### 13.3. Design Decisions & Layering Architecture

```text
┌────────────────────────────────────────────────────────────────────────┐
│                      ELEVATION LAYERING MAP                            │
├─────────────────┬─────────────┬──────────────────┬─────────────────────┤
│ Layer Level     │ Z-Index/Dp  │ Light Surface    │ Dark Luminance      │
├─────────────────┼─────────────┼──────────────────┼─────────────────────┤
│ Layer 0 (Canvas)│ 0dp         | #FAFAFA          │ #090D16             │
│ Layer 1 (Card)  │ 1dp (Flat)  | #FFFFFF + Border │ #161B26 + Border    │
│ Layer 2 (Raised)│ 2dp - 4dp   | #FFFFFF + Shadow │ #212838 + Border    │
│ Layer 3 (Overlay│ 8dp         | #FFFFFF + Shadow │ #2D3748 + Glow      │
│ Layer 4 (Modal) │ 16dp - 24dp | #FFFFFF + Scrim  │ #3F3F46 + Scrim     │
└─────────────────┴─────────────┴──────────────────┴─────────────────────┤
```

### 13.4. Specification Chi Tiết Các Elevation Layer:

1. **Layer 0 — Base Canvas (`elevation0`)**
   - Role: Nền đáy ứng dụng (Window Shell).
   - Light: `#FAFAFA` | Dark: `#090D16`. Zero shadow, zero border.

2. **Layer 1 — Surface Primary (`elevation1`)**
   - Role: Thẻ nội dung chính (Study Card, Package Card).
   - Light: `#FFFFFF` với viền `borderSubtle` ($1\text{dp}$). Shadow: None.
   - Dark: `#161B26` với viền `borderSubtle` ($1\text{dp}$). Shadow: None.

3. **Layer 2 — Floating Controls / Hovered Cards (`elevation2`)**
   - Role: Thẻ khi di chuột qua (Hovered State), Toolbar cố định, Floating Rating Dock.
   - Light: `#FFFFFF` với bóng nhạt $2\text{dp}$ (`Color(0x0F000000)` blur $4\text{dp}$).
   - Dark: `#212838` nâng độ sáng bề mặt, viền `borderMedium`.

4. **Layer 3 — Dropdown Menu / Contextual Tooltip (`elevation3`)**
   - Role: Menu thả xuống, Tooltip hướng dẫn, Popover.
   - Light: `#FFFFFF` với bóng $8\text{dp}$ (`Color(0x1F000000)` blur $12\text{dp}$).
   - Dark: `#2D3748` với viền sáng `borderFocus` mỏng.

5. **Layer 4 — Modal Dialog & Scrim Overlay (`elevation4`)**
   - Role: Hộp thoại xác nhận, Cửa sổ thiết lập phím tắt.
   - Scrim: Lớp phủ tối toán màn hình `Color(0x80000000)` (Alpha 50%).
   - Container: `#FFFFFF` (Light) / `#3F3F46` (Dark), Z-index ưu tiên cao nhất.

### 13.5. Future Implementation Guidance
Tất cả các thành phần nổi phải tuân thủ Layering Map trên. Không sử dụng `elevation(100.dp)` bừa bãi trong Compose.

---

## 14. MOTION & TRANSITION SYSTEM

### 14.1. Objective
Quy định ngôn ngữ chuyển động (**Motion Language System**) dành riêng cho Desktop: Phản hồi siêu nhanh (Crisp & Responsive), không gây hoảng loạn thị giác, phục vụ trải nghiệm học tập tập trung.

### 14.2. Rationale
Khác với Mobile nơi ngón tay vuốt chuyển trang kéo dài, người dùng Desktop thao tác bằng phím tắt và chuột với tốc độ rất cao (gõ `Space` xem đáp án, bấm `1-4` đánh giá). Chuyển động trên Desktop phải diễn ra trong chớp mắt ($80\text{ms} - 200\text{ms}$), nếu chuyển động quá chậm ($\ge 400\text{ms}$) sẽ gây cảm giác "ì ạch" (UI lag) và làm ngắt quãng dòng suy nghĩ (Flow State).

### 14.3. Design Decisions & Motion Duration Scale

| Duration Token | Value (ms) | Usage Guidelines & Motion Context |
| :--- | :--- | :--- |
| `durationInstant` | `0ms` | Chuyển trạng thái tức thì: Duyệt phím Tab, thay đổi màu text khi gõ phím. |
| `durationVeryFast`| `80ms` | **Micro interactions:** Hover button, Press button scale, Focus ring outline fade. |
| `durationFast`    | `120ms` | **Control state transitions:** Tooltip appear/disappear, Checkbox toggle, Dropdown menu open. |
| `durationNormal`  | `200ms` | **Content Reveal & Transitions:** Reveal Answer expansion, Rating feedback confirmation, Undo toast slide. |
| `durationSlow`    | `300ms` | **Structural Layout Shifts:** Expand/Collapse Example Card, Modal Dialog Fade-in/Fade-out. |
| `durationVerySlow`| `400ms` | **Loading Skeletons:** Skeleton pulse wave animation, Page crossfade navigation. |

### 14.4. Easing Curves Specification
* **Standard Easing (`easingStandard`):** `CubicBezier(0.2, 0.0, 0.0, 1.0)` — Dùng cho hầu hết các chuyển động chuyển giao nội dung (Reveal, Expand).
* **Deceleration Easing (`easingDecelerate`):** `CubicBezier(0.0, 0.0, 0.2, 1.0)` — Dùng cho đối tượng đi từ ngoài vào màn hình (Dialog enter, Tooltip popup).
* **Acceleration Easing (`easingAccelerate`):** `CubicBezier(0.4, 0.0, 1.0, 1.0)` — Dùng cho đối tượng biến mất khỏi màn hình (Dialog exit, Toast dismiss).

### 14.5. Motion Context Rules (Quy tắc chuyển động theo ngữ cảnh)
1. **Reveal Answer Transition:** Khi ấn `Space`, khu vực Answer hiển thị bằng hiệu ứng `FadeIn(200ms) + ExpandVertically(200ms, easingStandard)`. Zero hiệu ứng lật thẻ 3D rườm rà.
2. **Rating Feedback Animation:** Khi ấn phím `1-4`, Nút Rating tương ứng chớp nhẹ màu Semantic trong $120\text{ms}$ rồi chuyển ngay sang từ tiếp theo.
3. **Undo Toast Notification:** Trượt nhẹ từ phía dưới lên $16\text{dp}$ kết hợp `FadeIn` trong $200\text{ms}$.

### 14.6. Future Implementation Guidance
Tất cả `animationSpec` trong Compose phải sử dụng `tween(durationMillis = LEMotion.durationNormal, easing = LEMotion.easingStandard)`. Nghiêm cấm viết hằng số duration tự do.

---

## 15. ICONOGRAPHY SYSTEM

### 15.1. Objective
Chuẩn hóa hệ thống biểu tượng (**Iconography System**) đồng bộ về Optical Size, Stroke Width, Bounding Box và Ý nghĩa Nhận thức.

### 15.2. Rationale
Biểu tượng không phải là hình minh họa trang trí. Icon là **Ký hiệu thị giác rút gọn (Visual Shorthand)** giúp người dùng Desktop quét nhanh tính năng mà không cần đọc chữ. Icon không đồng bộ về nét vẽ (stroke) hay độ dày sẽ gây cảm giác thiếu chuyên nghiệp.

### 15.3. Design Decisions & Icon Classification

| Icon Role Category | Optical Size | Stroke Width | Alignment & Padding | Target Usage |
| :--- | :--- | :--- | :--- | :--- |
| `iconPrimary` | `24dp` | `2.0dp` | Centered in 24dp box | Icon thanh tiêu đề, Icon màn hình chính. |
| `iconSecondary` | `20dp` | `1.75dp` | Centered in 20dp box | Icon trong Button, Icon ô nhập liệu Search. |
| `iconMetadata` | `16dp` | `1.5dp` | Centered in 16dp box | Icon bé kế bên IPA, Audio play icon inline, Tag icon. |
| `iconStatus` | `16dp` / `20dp` | `2.0dp` | Centered in box | Icon trạng thái Valid (Check), Missing (Alert), Warning. |
| `iconDisabled` | `16dp` / `20dp` | `1.5dp` | Centered in box | Icon ở trạng thái vô hiệu hóa (Opacity $0.4$). |

### 15.4. Specific Domain Icon Guidelines
* **Audio Icon (`iconAudio`):** Hình loa đơn giản nét mảnh ($1.5\text{dp}$). Khi đang phát âm thanh, hiển thị sóng âm `waveformActive`.
* **Learning Stage Icon (`iconLearning`):** Icon hình ngọn lửa/mặt trời đại diện cho các item đang học.
* **Scheduler FSRS Icon (`iconScheduler`):** Icon đồng hồ/lịch đại diện cho thời gian ôn tập lặp lại.

### 15.5. Future Implementation Guidance
Toàn bộ Icon phải tuân thủ bộ Vector chuẩn (Lucide Icons / Feather Icons hoặc Material Symbols Outlined $2.0\text{dp}$ stroke). Nghiêm cấm dùng icon có nét vẽ khác nhau (trộn fill và outline).

---

## 16. COMPONENT STATE MATRIX

### 16.1. Objective
Xây dựng **Ma trận Trạng thái Component (Component State Matrix)** đầy đủ cho 14 trạng thái tương tác của mọi điều khiển UI trong hệ thống.

### 16.2. Rationale
Lỗi giao diện thường xảy ra khi kỹ sư chỉ thiết kế trạng thái bình thường (Resting) mà quên mất các trạng thái biên như Disabled, Loading, Focused, Error. Ma trận này đảm bảo mọi Component khi được xây dựng đều có phản hồi thị giác chính xác cho mọi trường hợp tương tác.

### 16.3. State Matrix Table Specification

| Component | Rest | Hover | Pressed | Focused | Selected | Disabled | Loading | Error |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **LEButton (Primary)** | `accentPrimary` | `accentHover` | Accent Dark | Viền `borderFocus` $2\text{dp}$ | N/A | Gray 400 | Spinner + Text mờ | Red Border |
| **LEButton (Secondary)** | `surfaceSecondary` | Surface Darker | Surface Darkest | Viền `borderFocus` $2\text{dp}$ | N/A | Gray 300 | Spinner + Text mờ | Red Text |
| **Study Rating Button** | Semantic Tint | Semantic Hover | Semantic Dark | Viền Focus $2\text{dp}$ | Active State | Gray 300 | N/A | N/A |
| **Meaning / Example Card**| `surfacePrimary` | Surface +5% | Scale 99.5% | Viền Focus $2\text{dp}$ | Active Border | Muted Opacity | Skeleton Pulse | Red Border |
| **Input Text Field** | `borderSubtle` | `borderMedium` | `borderFocus` | Viền `borderFocus` $2\text{dp}$ | Text Selected | Gray 200 | Loading Icon | `danger` Border |
| **Shortcut Badge** | `surfaceSecondary` | Surface Darker | Pressed Tone | Highlight Focus | Active State | Text Muted | N/A | N/A |
| **Status Badge** | Semantic Container | Hover Container | Pressed Container | Focus Ring | Active Tag | Gray Container | N/A | Red Container |

### 16.4. Future Implementation Guidance
Mọi Base Control Composable khi được code ở PLE-028C phải implement đủ các branch state trong Ma trận trên.

---

## 17. FOCUS SYSTEM & KEYBOARD NAVIGATION

### 17.1. Objective
Định nghĩa hệ thống tập trung bàn phím (**Desktop Focus System**) đảm bảo tính tiếp cận 100% qua phím Tab/Arrows, khả năng phục hồi Focus (Focus Recovery) và chỉ báo vị trí cực kỳ rõ nét.

### 17.2. Rationale
Learning Engine là ứng dụng **Keyboard-First Learning Workspace**. Người học có thể hoàn thành cả phiên học 50 từ vựng mà không chạm vào chuột. Do đó, Focus System là sinh mạng của trải nghiệm người dùng Desktop.

### 17.3. Design Decisions & Focus Ring Architecture
* **Focus Outline Style:** Double Stroke Ring (Viền kép). Viền trong suốt $2\text{dp}$ để tách biệt khỏi thẻ + Viền ngoài $2\text{dp}$ màu Accent nổi bật.
* **Light Theme Focus Ring Color:** `accentPrimary` (`#6D28D9`).
* **Dark Theme Focus Ring Color:** `borderFocus` (`#A78BFA` - Tím sáng dạ quang).
* **Focus Outline Offset:** Gap $2\text{dp}$ (`OutlineOffset = 2.dp`).
* **Focus Transition:** Xuất hiện nhanh trong $80\text{ms}$ (`durationVeryFast`).

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        KEYBOARD FOCUS RING                             │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │  Outer Focus Ring (2dp Vivid Accent Color)                     │   │
│   │   ┌────────────────────────────────────────────────────────┐   │   │
│   │   │  Clear Gap Offset (2dp Transparent Space)              │   │   │
│   │   │   ┌────────────────────────────────────────────────┐   │   │   │
│   │   │   │  Actual Target Control (Button / Input Field)  │   │   │   │
│   │   │   └────────────────────────────────────────────────┘   │   │   │
│   │   └────────────────────────────────────────────────────────┘   │   │
│   └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

### 17.4. Focus Retention & Recovery Strategy
1. **Focus Retention across Reveal:** Khi chuyển từ Question sang Answer qua phím `Space`, Focus vẫn giữ nguyên tại `StudyWorkspaceSurface` để người học bấm tiếp phím `1-4` mà không bị rơi Focus ra ngoài.
2. **Focus Recovery after Dialog Dismiss:** Khi đóng Hộp thoại Settings hoặc Modal bất kỳ bằng phím `Esc`, Focus tự động hoàn trả về vị trí phần tử đã kích hoạt Dialog trước đó.

### 17.5. Future Implementation Guidance
Triển khai bằng `Modifier.focusProperties` và `Modifier.border` tùy chỉnh trong Compose, đáp ứng hoàn toàn tiêu chuẩn **WCAG 2.1 Criterion 2.4.7 (Focus Visible)**.

---

## 18. DENSITY SYSTEM

### 18.1. Objective
Quy định **Hệ thống Mật độ Hiển thị (Density System)** cho phép ứng dụng linh hoạt thay đổi giữa trải nghiệm học thoải mái (Comfort Mode) và trải nghiệm mật độ cao (Compact Mode).

### 18.2. Rationale
Người học trên màn hình laptop 13-inch cần không gian hiển thị vừa vặn, trong khi người học trên màn hình desktop 27-inch hoặc các chuyên gia cần hiển thị tối đa dữ liệu danh sách bài học mà không muốn cuộn trang nhiều.

### 18.3. Density Modes Specification

| Mode Name | Target User & Screen Context | Target Hit Area (Min) | Spacing Multiplier | Card Padding |
| :--- | :--- | :--- | :--- | :--- |
| `densityComfort` | **Standard Study Mode (Default):** Học từ vựng tập trung, dịu mắt. | `40dp` x `40dp` | `1.0x` (Standard Scale) | `16dp` (`space5`) |
| `densityCompact` | **Power User / Library List / Data Studio:** Duyệt danh sách nhiều bài. | `32dp` x `32dp` | `0.75x` (Compact Scale) | `12dp` (`space4`) |
| `densityTouch` | **Future Touch / Tablet support:** Dành cho thiết bị cảm ứng tương lai. | `48dp` x `48dp` | `1.25x` (Large Scale) | `24dp` (`space6`) |

### 18.4. Density Switch Guidance
Hệ thống token tự động tính toán Padding và Control Size thông qua `LETheme.density` mà không làm thay đổi logic hiển thị của Component.

---

## 19. IMAGE SYSTEM

### 19.1. Objective
Quy định chuẩn hóa việc hiển thị hình ảnh phương tiện học tập (**Adaptive Image System**): Xử lý các trạng thái tải, ảnh thiếu (missing), ảnh lỗi (failed), căn mép và tỷ lệ khung hình.

### 19.2. Rationale
Hình ảnh minh họa từ vựng (Studio Hero Image / Prompt Image) đóng vai trò quan trọng trong việc kích thích trí nhớ thị giác (Visual Memory). Tuy nhiên, ảnh có kích thước và tỷ lệ rất đa dạng. Việc thiếu chuẩn hóa làm hình ảnh vỡ khung, đè chữ hoặc gây giật màn hình khi tải.

### 19.3. Design Decisions & Image Specification
* **Aspect Ratio Bounding:** Tỷ lệ khung hình ưu tiên $16:9$ hoặc $4:3$. Chiều cao tối đa (Max Height) được giới hạn linh hoạt theo Viewport (Tối đa $240\text{dp}$ trên màn hình Standard, $180\text{dp}$ trên màn hình Compact).
* **Cropping Policy:** Dùng `ContentScale.Fit` để giữ trọn vẹn chi tiết minh họa từ vựng, không cắt xén (crop) mất thông tin ảnh trừ khi được chỉ định rõ `ContentScale.Crop`.
* **Border & Shape:** Hình ảnh luôn được bọc trong container bo góc `radiusM` ($8\text{dp}$) với viền mỏng `borderSubtle` ($1\text{dp}$) để không bị lẫn vào màu nền.
* **Placeholder & Loading State:** Khi ảnh đang tải, hiển thị khung chữ nhật màu `surfaceSecondary` với hiệu ứng sóng mờ (Skeleton Pulse `durationVerySlow`).
* **Missing / Failed State:** Khi không có ảnh hoặc tải lỗi, hiển thị container phẳng gọn nhẹ với Icon `iconMetadata` hình ảnh thiếu dạng mờ, tuyệt đối không làm vỡ bố cục văn bản.

### 19.4. Future Implementation Guidance
Triển khai qua `StudioHeroImage` Composable kết hợp với trình tải Skia/ImageBitmap của Desktop Compose.

---

## 20. THEME INDEPENDENCE CONTRACT (BẢN HỢP ĐỒNG ĐỘC LẬP THEME)

### 20.1. Objective
Quy định **Bản Hợp Đồng Kiến Trúc Bắt Buộc (Strict Architectural Contract)** ngăn chặn 100% việc hardcode mã màu hoặc giá trị giao diện trực tiếp bên trong bất kỳ Component hay Màn hình nào.

### 20.2. Rationale & Core Architecture Law
Nếu một lập trình viên viết trực tiếp `Color(0xFF6D28D9)` hay `Color.White` vào trong một Composable file, ứng dụng sẽ ngay lập tức bị vỡ giao diện khi người dùng chuyển sang Dark Theme hoặc khi hệ thống thay đổi Design Language.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                   THEME INDEPENDENCE FLOW CONTRACT                     │
│                                                                        │
│   Component UI Code (e.g. StudyScreen.kt, LECard.kt)                   │
│         │                                                              │
│         ▼ (Chỉ được phép truy vấn)                                     │
│   Semantic Theme Tokens (e.g. LEColors.surfacePrimary, LEText.display) │
│         │                                                              │
│         ▼ (Được giải mã tự động qua)                                   │
│   Theme Resolver Context (LETheme.colors, LETheme.typography)          │
│         │                                                              │
│         ▼ (Trả về giá trị màu thực tế)                                 │
│   Actual Concrete Color Value (#FFFFFF in Light / #161B26 in Dark)      │
└────────────────────────────────────────────────────────────────────────┘
```

### 20.3. Strict Rules of Theme Independence (Các quy định nghiêm ngặt):
1. **NGHIÊM CẤM 100%:** Component $\rightarrow$ Hex Color (`Color(0xFF...)`).
2. **NGHIÊM CẤM 100%:** Component $\rightarrow$ Native Material 3 Direct Color (`MaterialTheme.colorScheme.primary`).
3. **BẮT BUỘC:** Component $\rightarrow$ `LEColors.[semanticTokenName]`.
4. Mọi màu sắc hiển thị trên màn hình đều phải tự động thích ứng khi đổi giữa Light Theme và Dark Theme mà không cần bất kỳ câu lệnh `if (isDark)` nào ở tầng Component UI.

### 20.4. Enforcement Guidance
Viết Unit Test kiểm tra mã nguồn Compose UI để đảm bảo không chứa các ký tự hex `0xFF` ngoài file `LEColors.kt`.

---

## 21. DESIGN SYSTEM GOVERNANCE

### 21.1. Objective
Quy định **Quy trình Quản trị & Phát triển Design System (Governance Policy)** cho toàn bộ đội ngũ phát triển Learning Engine 2.0.

### 21.2. Governance & Review Checklist
Trước khi thêm bất kỳ Token hoặc Component UI mới nào vào codebase, phải vượt qua **5 bước Review Checklist**:
1. **Reuse Check:** Khái niệm thị giác này có thể tái sử dụng từ Token/Component hiện có không? (Nếu có $\rightarrow$ Bắt buộc dùng lại).
2. **Semantic Check:** Naming của Token mới có phản ánh đúng ý nghĩa chức năng (Functional Intent) thay vì phản ánh màu sắc cụ thể không? (Ví dụ: Dùng `borderFocus` thay vì `purpleBorder`).
3. **Contrast Check:** Token màu mới có đạt chuẩn WCAG 2.1 AA/AAA trên cả Light Theme và Dark Theme không?
4. **Theme Independence Check:** Component có hoàn toàn độc lập với Hex color không?
5. **Documentation Check:** Đã cập nhật Specification tại `PLE-028A_VISUAL_THEME_SYSTEM.md` chưa?

### 21.3. Naming Convention Rules
- **Color Tokens:** `[category][Role][Variant]` (Ví dụ: `surfacePrimary`, `textSecondary`, `borderFocus`, `statusSuccessContainer`).
- **Typography Tokens:** `[role][Context]` (Ví dụ: `displayWord`, `meaningPrimary`, `shortcutBadge`).
- **Spacing Tokens:** `space[ScaleIndex]` (Ví dụ: `space1`, `space4`, `space8`).
- **Shape Tokens:** `radius[Size]` (Ví dụ: `radiusS`, `radiusL`, `radiusPill`).

### 21.4. Breaking Change Policy & Migration Strategy
- Không bao giờ xóa trực tiếp một Token đang tồn tại.
- Khi cần thay đổi Token: Đánh dấu `@Deprecated` kèm theo chỉ dẫn Token thay thế trong 1 Milestone, và chỉ xóa bỏ hoàn toàn ở Milestone tiếp theo sau khi đã hoàn tất Migration.

---

## 22. SUMMARY & CONCLUSION

Tài liệu **Visual Theme System Foundation Specification (PLE-028A & PLE-028A.1)** này là **Hiến pháp Thiết kế (Design Constitution)** hoàn chỉnh nhất của Learning Engine 2.0. Với 21 chương kiến trúc phủ kín từ Nguyên lý, Màu sắc, Typography, Spacing, Shape, Elevation, Motion, Iconography, State Matrix, Focus, Density, Image System cho tới Bản hợp đồng Độc lập Theme và Quy trình Quản trị, tài liệu này đảm bảo bất kỳ kỹ sư nào cũng có thể phát triển giao diện chuẩn mực, nhất quán, tiếp cận cao và đẹp mắt mà không cần phải tự đưa ra các quyết định cảm tính.
