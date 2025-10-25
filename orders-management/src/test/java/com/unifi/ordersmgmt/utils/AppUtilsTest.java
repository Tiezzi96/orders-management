package com.unifi.ordersmgmt.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;

import org.junit.Test;

public class AppUtilsTest {

	@Test
	public void testIsValidDateShouldReturnTrueForValidDate() {
		assertThat(AppUtils.isValidDate(30, 9, 2020)).isTrue();
	}

	@Test
	public void testIsValidDateShouldReturnTrueForEndOfYearDate() {
		assertThat(AppUtils.isValidDate(31, 12, 2020)).isTrue();
	}

	@Test
	public void testIsValidDateShouldReturnTrueForValidLeapDayInLeapYear() {
		assertThat(AppUtils.isValidDate(29, 2, 2000)).isTrue();
	}

	@Test
	public void testIsValidDateShouldReturnFalseForInvalidLeapDayInNonLeapYear() {
		assertThat(AppUtils.isValidDate(29, 2, 2001)).isFalse();
	}

	@Test
	public void testIsValidDateShouldReturnFalseForInvalidDateInMonth() {
		assertThat(AppUtils.isValidDate(31, 9, 2020)).isFalse();
	}

	@Test
	public void testIsValidDateShouldReturnFalseForDayZero() {
		assertThat(AppUtils.isValidDate(0, 9, 2000)).isFalse();
	}

	@Test
	public void testIsValidDateShouldReturnFalseForDayAboveMax() {
		assertThat(AppUtils.isValidDate(32, 10, 2000)).isFalse();
	}

	@Test
	public void testIsValidDateShouldReturnFalseForMonthAboveMax() {
		assertThat(AppUtils.isValidDate(30, 13, 2000)).isFalse();
	}

	@Test
	public void testIsValidDateShouldReturnFalseForMonthZero() {
		assertThat(AppUtils.isValidDate(30, 0, 2000)).isFalse();
	}

	@Test
	public void testIsValidDateShouldReturnFalseForLeapDayInYear1900_NotLeapYear() {
		assertThat(AppUtils.isValidDate(29, 2, 1900)).isFalse();
	}

	@Test
	public void testIsValidDateShouldValidateMinYearBoundary() {
		assertThat(AppUtils.isValidDate(1, 1, 1925)).isTrue();
		assertThat(AppUtils.isValidDate(1, 1, 1924)).isFalse();
	}

	@Test
	public void testIsValidDateShouldValidateMaxYearBoundary() {
		assertThat(AppUtils.isValidDate(31, 12, 2025)).isTrue();
		assertThat(AppUtils.isValidDate(1, 1, 2026)).isFalse();
	}

	@Test
	public void testIsValidDateShouldReturnTrueForLeapDayIn2024() {
		assertThat(AppUtils.isValidDate(29, 2, 2024)).isTrue();
	}

	@Test
	public void testIsValidDateShouldReturnFalseForLeapDayIn2023_NotLeapYear() {
		assertThat(AppUtils.isValidDate(29, 2, 2023)).isFalse();
	}

	@Test
	public void testIsValidDateShouldReturnFalseForLeapDayIn2100_NotLeapYear() {
		assertThat(AppUtils.isValidDate(29, 2, 2100)).isFalse();
	}

	@Test
	public void testIsLeapYearShouldCorrectlyDetectLeapYears() {
		assertThat(AppUtils.isLeapYear(2100)).isFalse();
		assertThat(AppUtils.isLeapYear(2000)).isTrue();
		assertThat(AppUtils.isLeapYear(2024)).isTrue();
		assertThat(AppUtils.isLeapYear(1900)).isFalse();
		assertThat(AppUtils.isLeapYear(2023)).isFalse();
	}

	@Test
	public void testShouldInstantiatePrivateConstructorForCoverage() throws Exception {
		Constructor<AppUtils> constructor = AppUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		AppUtils instance = constructor.newInstance();
		assertThat(instance).isNotNull().isInstanceOf(AppUtils.class);
	}

}
