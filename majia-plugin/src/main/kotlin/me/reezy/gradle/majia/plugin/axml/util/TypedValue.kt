package me.reezy.gradle.majia.plugin.axml.util

object TypedValue {
    /** The value contains no data. */
    const val TYPE_NULL = 0x00

    /** The <var>data</var> field holds a resource identifier. */
    const val TYPE_REFERENCE = 0x01

    /**
     * The <var>data</var> field holds an attribute resource identifier
     * (referencing an attribute in the current theme style, not a resource
     * entry).
     */
    const val TYPE_ATTRIBUTE = 0x02

    /**
     * The <var>string</var> field holds string data. In addition, if
     * <var>data</var> is non-zero then it is the string block index of the
     * string and <var>assetCookie</var> is the set of assets the string came
     * from.
     */
    const val TYPE_STRING = 0x03

    /** The <var>data</var> field holds an IEEE 754 floating point number. */
    const val TYPE_FLOAT = 0x04

    /**
     * The <var>data</var> field holds a complex number encoding a dimension
     * value.
     */
    const val TYPE_DIMENSION = 0x05

    /**
     * The <var>data</var> field holds a complex number encoding a fraction of
     * a container.
     */
    const val TYPE_FRACTION = 0x06

    /**
     * Identifies the start of plain integer values. Any type value from this
     * to [.TYPE_LAST_INT] means the <var>data</var> field holds a generic
     * integer value.
     */
    const val TYPE_FIRST_INT = 0x10

    /**
     * The <var>data</var> field holds a number that was originally specified
     * in decimal.
     */
    const val TYPE_INT_DEC = 0x10

    /**
     * The <var>data</var> field holds a number that was originally specified
     * in hexadecimal (0xn).
     */
    const val TYPE_INT_HEX = 0x11

    /**
     * The <var>data</var> field holds 0 or 1 that was originally specified as
     * "false" or "true".
     */
    const val TYPE_INT_BOOLEAN = 0x12

    /**
     * Identifies the start of integer values that were specified as color
     * constants (starting with '#').
     */
    const val TYPE_FIRST_COLOR_INT = 0x1c

    /**
     * Identifies the end of integer values that were specified as color
     * constants.
     */
    const val TYPE_LAST_COLOR_INT = 0x1f

    /** Identifies the end of plain integer values. */
    const val TYPE_LAST_INT = 0x1f
    /* ------------------------------------------------------------ */
    /** Complex data: bit location of unit information. */
    const val COMPLEX_UNIT_SHIFT = 0

    /**
     * Complex data: mask to extract unit information (after shifting by
     * [.COMPLEX_UNIT_SHIFT]). This gives us 16 possible types, as defined
     * below.
     */
    const val COMPLEX_UNIT_MASK = 0xf

    /**
     * Complex data: where the radix information is, telling where the decimal
     * place appears in the mantissa.
     */
    const val COMPLEX_RADIX_SHIFT = 4

    /**
     * Complex data: mask to extract radix information (after shifting
     * by [.COMPLEX_RADIX_SHIFT]). This give us 4 possible fixed point
     * representations as defined below.
     */
    const val COMPLEX_RADIX_MASK = 0x3

    /** Complex data: bit location of mantissa information. */
    const val COMPLEX_MANTISSA_SHIFT = 8

    /**
     * Complex data: mask to extract mantissa information (after shifting by
     * [.COMPLEX_MANTISSA_SHIFT]). This gives us 23 bits of precision; the top
     * bit is the sign.
     */
    const val COMPLEX_MANTISSA_MASK = 0xffffff
    private const val MANTISSA_MULT = 1.0f / (1 shl COMPLEX_MANTISSA_SHIFT)
    private val RADIX_MULTS = floatArrayOf(
        1.0f * MANTISSA_MULT, 1.0f / (1 shl 7) * MANTISSA_MULT,
        1.0f / (1 shl 15) * MANTISSA_MULT, 1.0f / (1 shl 23) * MANTISSA_MULT
    )

    /**
     * Retrieve the base value from a complex data integer. This uses the
     * [.COMPLEX_MANTISSA_MASK] and [.COMPLEX_RADIX_MASK] fields of the data to
     * compute a floating point representation of the number they describe. The
     * units are ignored.
     *
     * @param complex A complex data value.
     * @return A floating point value corresponding to the complex data.
     */
    @JvmStatic
    private fun complexToFloat(complex: Int): Float {
        return ((complex and (COMPLEX_MANTISSA_MASK shl COMPLEX_MANTISSA_SHIFT)) * RADIX_MULTS[complex shr COMPLEX_RADIX_SHIFT and COMPLEX_RADIX_MASK])
    }

    private val DIMENSION_UNITS = arrayOf("px", "dip", "sp", "pt", "in", "mm")
    private val FRACTION_UNITS = arrayOf("%", "%p")

    /**
     * Perform type conversion as per [.coerceToString] on an explicitly
     * supplied type and data.
     *
     * @param type The data type identifier.
     * @param data The data value.
     * @return String The coerced string value. If the value is null or the
     *     type is not known, null is returned.
     */
    @JvmStatic
    fun coerceToString(type: Int, data: Int): String? {
        when (type) {
            TYPE_NULL -> return null
            TYPE_REFERENCE -> return "@$data"
            TYPE_ATTRIBUTE -> return "?$data"
            TYPE_FLOAT -> return java.lang.Float.intBitsToFloat(data).toString()
            TYPE_DIMENSION -> return complexToFloat(data).toString() + DIMENSION_UNITS[data shr COMPLEX_UNIT_SHIFT and COMPLEX_UNIT_MASK]
            TYPE_FRACTION -> return (complexToFloat(data) * 100).toString() + FRACTION_UNITS[data shr COMPLEX_UNIT_SHIFT and COMPLEX_UNIT_MASK]
            TYPE_INT_HEX -> return "0x" + Integer.toHexString(data)
            TYPE_INT_BOOLEAN -> return if (data != 0) "true" else "false"
        }
        if (type in TYPE_FIRST_COLOR_INT..TYPE_LAST_COLOR_INT) {
            return "#" + Integer.toHexString(data)
        } else if (type in TYPE_FIRST_INT..TYPE_LAST_INT) {
            return data.toString()
        }
        return null
    }
}