package com.example.wllpr

data class Section(val pattern: Int, val title: String, val subtitle: String)

data class Wallpaper(
    val id: String,
    val name: String,
    val section: Int,
    val spec: Spec,
    val featured: Boolean = false,
) {
    val modeLabel: String get() = if (spec.inverted) "Light" else "Dark"
    val paletteName: String get() = Palettes.all[spec.palette].name
    val patternName: String get() = PATTERN_LABELS[spec.pattern]
}

object Catalog {
    /** Order the sections appear on the home screen. */
    val sections = listOf(
        Section(4, "Arcs", "Concentric shapes and gradients"),
        Section(0, "Hills", "Soft landscapes with pine trees"),
        Section(1, "Waves", "Fluid, smooth curves"),
        Section(3, "Mountains", "Rugged, dramatic peaks"),
        Section(2, "Dunes", "Organic, rolling textures"),
        Section(5, "Scribble", "Abstract strokes and free curves"),
    )

    private val seeds = mapOf(0 to 2718, 1 to 3141, 2 to 5772, 3 to 1618, 4 to 4669, 5 to 8811)

    /** Order palettes are listed inside every section. */
    private val paletteOrder = listOf(4, 5, 6, 8, 11, 2, 10, 7, 9, 3, 0, 1, 12, 13)

    val all: List<Wallpaper> = sections.flatMap { s ->
        listOf(true, false).flatMap { inverted ->
            paletteOrder.map { p ->
                val pal = Palettes.all[p]
                val mode = if (inverted) "light" else "dark"
                Wallpaper(
                    id = "${s.title.lowercase()}-$p-$mode",
                    name = "${s.title} ${pal.name}",
                    section = s.pattern,
                    spec = Spec(s.pattern, p, seeds.getValue(s.pattern), inverted),
                    featured = s.pattern == 1 && p == 6 && inverted,
                )
            }
        }
    }

    private val byId = all.associateBy { it.id }

    fun get(id: String): Wallpaper? = byId[id]

    val featured: Wallpaper get() = all.firstOrNull { it.featured } ?: all.first()
}
