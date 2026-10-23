package com.example.bodegalider.util

import java.text.Normalizer

/** Pasa a minúsculas y quita tildes/espacios extra: "  María " -> "maria". */
fun String.normalizeForSearch(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("\\s+"), " ")
