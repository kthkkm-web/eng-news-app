package app.myroutine.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 루틴 색상: "색상 테마"(5색 묶음) 중 하나를 고르고, 그 안에서 루틴 색 1개를 고른다.
 * 저장 형식은 "themeId:index" (예: "basic:2"). 테마/색을 바꿔도 키는 유지되므로 안전.
 */
data class PaletteTheme(val id: String, val name: String, val colors: List<Long>)

object RoutinePalettes {
    val themes: List<PaletteTheme> = listOf(
        PaletteTheme("basic", "기본", listOf(0xFF9B87E8, 0xFF2FB39A, 0xFFF5B819, 0xFFEE8FB8, 0xFF35B6EC)),
        PaletteTheme("vivid", "선명", listOf(0xFFE04F9B, 0xFFF0532A, 0xFF22A05A, 0xFFF7C711, 0xFF2F64D8)),
        PaletteTheme("retro", "레트로", listOf(0xFFD4B5E0, 0xFFE5532A, 0xFF4458A8, 0xFF2CA58E, 0xFFF6C3AC)),
        PaletteTheme("candy", "캔디", listOf(0xFFF7C8D4, 0xFFDCE35B, 0xFFF39321, 0xFFF0609E, 0xFF4E7A2E)),
        PaletteTheme("forest", "숲", listOf(0xFF4E8A5C, 0xFFE2D7B8, 0xFF6F8F7F, 0xFFB9D4CF, 0xFF7BA39C)),
        PaletteTheme("ocean", "바다", listOf(0xFF2E6A8A, 0xFF74AFC0, 0xFFB7D7D2, 0xFF7E9A6E, 0xFFF4A259)),
        PaletteTheme("mist", "안개", listOf(0xFFA8C9BC, 0xFF46708C, 0xFF5E9AA6, 0xFFDDE6B5, 0xFFA9C4E2)),
        PaletteTheme("meadow", "들판", listOf(0xFFE07C84, 0xFF5D7F45, 0xFFF7D2B0, 0xFFB5B46E, 0xFFDCEB95)),
        PaletteTheme("sunset", "노을", listOf(0xFFFF8A5B, 0xFFFFB86B, 0xFFE85D75, 0xFF9B5DE5, 0xFF5D7BE8)),
        PaletteTheme("pastel", "파스텔", listOf(0xFFB8C0FF, 0xFFBDE0FE, 0xFFC8F2D4, 0xFFFFE5A8, 0xFFFFC6D9)),
        PaletteTheme("mono", "모노", listOf(0xFFE4E6E8, 0xFFB6BBC0, 0xFF8A9096, 0xFF61676D, 0xFF8FA8BF)),
        PaletteTheme("neon", "네온", listOf(0xFF39FF88, 0xFF00E5FF, 0xFFFF3DCB, 0xFFFFE53D, 0xFFFF6B3D)),
    )

    const val DEFAULT_KEY = "basic:1"

    fun key(themeId: String, index: Int) = "$themeId:$index"

    fun parse(key: String): Pair<PaletteTheme, Int> {
        val (tid, idx) = key.split(':').let { (it.getOrNull(0) ?: "basic") to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
        val theme = themes.firstOrNull { it.id == tid } ?: themes.first()
        return theme to idx.coerceIn(0, theme.colors.lastIndex)
    }

    fun argb(key: String): Long = parse(key).let { (t, i) -> t.colors[i] }

    fun color(key: String): Color = Color(argb(key))

    /** 홈 추가 화면에서 새 루틴에 돌아가며 기본 색 배정. */
    fun defaultFor(index: Int): String = key("basic", index % 5)
}
