package com.yzc.bookmall;

import static org.junit.Assert.assertEquals;

import com.yzc.bookmall.util.FormatUtils;

import org.junit.Test;

public class FormatUtilsTest {

    @Test
    public void formatPriceUsesYuanAndTwoDecimals() {
        assertEquals("¥0.00", FormatUtils.formatPrice(0));
        assertEquals("¥12.99", FormatUtils.formatPrice(1299));
        assertEquals("¥100.00", FormatUtils.formatPrice(10000));
    }

    @Test
    public void yuanReturnsPlainAmount() {
        assertEquals("0.00", FormatUtils.yuan(0));
        assertEquals("59.00", FormatUtils.yuan(5900));
        assertEquals("168.00", FormatUtils.yuan(16800));
    }

    @Test
    public void starsClampsToOneToFive() {
        assertEquals("★★★★★", FormatUtils.stars(5));
        assertEquals("★★★☆☆", FormatUtils.stars(3));
        assertEquals("☆☆☆☆☆", FormatUtils.stars(0));
        assertEquals("★★★★★", FormatUtils.stars(9));
    }

    @Test
    public void ratingKeepsOneDecimal() {
        assertEquals("4.6", FormatUtils.rating(4.56));
        assertEquals("5.0", FormatUtils.rating(5));
    }
}
