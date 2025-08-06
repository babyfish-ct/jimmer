package org.babyfish.jimmer.ksp.util


/**
 * 检查字符串是否不在列表中（不区分大小写）
 */
infix fun String.ignoreCaseNotIn(collection: Collection<String>): Boolean {
    val b = this ignoreCaseIn collection
    return !b
}

fun String.toBigCamelCase(): String {
    return this.split("_").joinToString("") {
        it.replaceFirstChar { char -> char.uppercase() }
    }
}

fun CharSequence.removeAnyQuote(): String {
    if (this.isBlank()) {
        return ""
    }
    return removeAny(this, "\"", "\\", "\\n", " ", System.lineSeparator())
}

/**
 * 从字符序列中移除所有指定的字符串片段
 *
 * @param str 原字符序列
 * @param stringsToRemove 要移除的字符串片段
 * @return 移除指定字符串片段后的新字符串
 */
fun removeAny(str: CharSequence, vararg stringsToRemove: String): String {
    var result = str.toString()
    for (toRemove in stringsToRemove) {
        result = result.replace(toRemove, "")
    }
    return result
}


/**
 * 将列名转换为驼峰命名
 */
fun String.toLowCamelCase(): String {
    return this.split("_").joinToString("") {
        it.replaceFirstChar { char -> char.uppercase() }
    }.replaceFirstChar { it.lowercase() }
}

/**
 * 字符串首字母小写
 */
fun String.lowerFirst(): String {
    if (isEmpty()) return this
    return first().lowercase() + substring(1)
}


/**
 * 检查字符序列是否包含任意一个给定的测试字符序列
 *
 * @param testStrs 要检查的测试字符序列集合
 * @return 如果包含任意一个测试字符序列则返回true，否则返回false
 */
fun CharSequence.containsAny(vararg testStrs: CharSequence): Boolean {
    if (this.isEmpty() || testStrs.isEmpty()) {
        return false
    }

    for (testStr in testStrs) {
        if (this.contains(testStr)) {
            return true
        }
    }

    return false
}

infix fun String.ignoreCaseLike(other: String): Boolean {
    return this.contains(other, ignoreCase = true)
}


fun String.toUnderLineCase(): String {
    val sb = StringBuilder()
    for ((index, char) in this.withIndex()) {
        if (index > 0 && char.isUpperCase()) {
            sb.append('_')
        }
        sb.append(char)
    }
    return sb.toString()
}

/**
 * 检查字符串是否在列表中（不区分大小写）
 */
infix fun String.ignoreCaseIn(collection: Collection<String>): Boolean =
    collection.any { it.equals(this, ignoreCase = true) }

/**
 * 检查字符串是否包含任意一个给定的子字符串（忽略大小写）
 * @param substrings 要检查的子字符串集合
 * @return 如果包含任意一个子字符串则返回true，否则返回false
 */
fun String.containsAnyIgnoreCase(vararg substrings: String): Boolean {
    if (substrings.isEmpty()) return false
    val lowerThis = this.lowercase()
    return substrings.any { it.lowercase() in lowerThis }
}


object JlStrUtil {

    /**
     * 清理字符串中的空白字符
     * 包括：
     * 1. 移除首尾空白字符
     * 2. 将连续的空白字符替换为单个空格
     * 3. 移除不可见字符
     *
     * @param str 要清理的字符串
     * @return 清理后的字符串
     */
    fun String?.cleanBlank(): String {
        if (this.isNullOrBlank()) return ""

        return this.trim()
            .replace(Regex("\\s+"), " ") // 将连续的空白字符替换为单个空格
            .filter { it.isDefined() } // 移除不可见字符
    }

    /**
     * 判断字符是否可见
     */
    private fun Char.isDefined(): Boolean {
        return this.code in 32..126 || this.code in 0x4E00..0x9FFF
    }

    /**
     * 删除字符串中最后一次出现的指定字符。
     * 注意这和removeSuffixifnot有所不同(该方法只是移除最后一个字符,而不是最后出现的字符,例如如最后一个是空格就翻车了)
     *
     * @param str 字符串
     * @param ch 要删除的字符
     * @return 删除指定字符后的字符串
     */
    fun removeLastCharOccurrence(str: String, ch: Char): String {
        if (str.isBlank()) {
            return ""
        }

        val lastIndex = str.lastIndexOf(ch) // 获取指定字符最后一次出现的位置
        return if (lastIndex != -1) {
            // 如果找到了指定字符，则删除它
            str.substring(0, lastIndex) + str.substring(lastIndex + 1)
        } else {
            // 如果没有找到指定字符，则返回原字符串
            str!!
        }
    }


    fun <T> groupBySeparator(lines: List<T>, predicate: (T) -> Boolean): Map<T, List<T>> {
        val separatorIndices = lines.indices.filter { predicate(lines[it]) }
        return separatorIndices.mapIndexed { index, spe ->
            val next = if (index + 1 < separatorIndices.size) {
                separatorIndices[index + 1]
            } else {
                lines.size // 如果没有下一个分隔符，取行的总数
            }

            val subList = lines.subList(spe + 1, next)
            lines[spe] to subList // 使用 Pair 进行配对
        }.toMap()
    }


    fun String.makeSurroundWith(fix: String): String {
        val addPrefixIfNot = this.addPrefixIfNot(fix)
        val addSuffixIfNot = addPrefixIfNot.addSuffixIfNot(fix)
        return addSuffixIfNot

    }


    /**
     * 如果字符串不以指定后缀结尾，则添加该后缀
     *
     * @param suffix 要添加的后缀
     * @param ignoreCase 是否忽略大小写，默认为false
     * @return 添加后缀后的字符串
     */
    fun String?.addSuffixIfNot(suffix: String, ignoreCase: Boolean = false): String {
        if (this == null) {
            return suffix
        }

        return if (this.endsWith(suffix, ignoreCase)) {
            this
        } else {
            this + suffix
        }
    }


}

fun String?.isBlank(): Boolean {
    if (this == null || this == "") {
        return true
    }
    return false
}

fun String?.isNotBlank(): Boolean {
    return !this.isBlank()
}

/**
 * 扩展函数：移除重复符号
 */
fun String?.removeDuplicateSymbol(duplicateElement: String): String {
    if (this.isNullOrEmpty() || duplicateElement.isEmpty()) {
        return this ?: ""
    }

    val sb = StringBuilder()
    var previous = "" // 初始化前一个元素，用于比较
    var i = 0

    while (i < this.length) {
        val elementLength = duplicateElement.length
        if (i + elementLength <= this.length && this.substring(i, i + elementLength) == duplicateElement) {
            if (previous != duplicateElement) {
                sb.append(duplicateElement)
                previous = duplicateElement
            }
            i += elementLength
        } else {
            sb.append(this[i])
            previous = this[i].toString()
            i++
        }
    }
    return sb.toString()
}

/**
 * 扩展函数：清理多余的char
 */
fun String?.removeDuplicateSymbol(symbol: Char): String {
    if (this.isBlank()) {
        return ""
    }
    val sb = StringBuilder()
    var prevIsSymbol = false

    for (c in this!!.toCharArray()) {
        if (c == symbol) {
            if (!prevIsSymbol) {
                sb.append(c)
                prevIsSymbol = true
            }
        } else {
            sb.append(c)
            prevIsSymbol = false
        }
    }
    return sb.toString()
}

/**
 * 扩展函数：提取路径部分
 */
fun String.getPathFromRight(n: Int): String? {
    val parts = this.split(".").filter { it.isNotEmpty() }

    if (parts!!.size < n) {
        return this // 输入字符串中的路径部分不足n个，返回整个输入字符串
    }

    return parts.dropLast(n).joinToString(".")
}


fun String?.lowerCase(): String {
    if (this.isBlank()) {
        return ""
    }
    val lowerCase = this.lowerCase()
    return lowerCase

}

/**
 * 如果字符串不以指定前缀开头，则添加该前缀
 *
 * @param prefix 要添加的前缀
 * @param ignoreCase 是否忽略大小写，默认为false
 * @return 添加前缀后的字符串
 */
fun String?.addPrefixIfNot(prefix: String, ignoreCase: Boolean = false): String {
    if (this.isNullOrBlank()) return prefix

    return if (ignoreCase) {
        if (this.startsWith(prefix, ignoreCase = true)) this else prefix + this
    } else {
        if (this.startsWith(prefix)) this else prefix + this
    }
}

