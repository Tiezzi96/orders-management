package com.unifi.ordersmgmt.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;

import org.junit.Test;

public class AppUtilsTest {

	@Test
	public void testShuoldReturnTrueForValidDate() {
		assertThat(AppUtils.isValidDate(30, 9, 2020)).isTrue();
	}

	@Test
	public void testShouldReturnTrueForEndOfYearDate() {
		assertThat(AppUtils.isValidDate(31, 12, 2020)).isTrue();
	}

	@Test
	public void shouldReturnTrueForValidLeapDayInLeapYear() {
		assertThat(AppUtils.isValidDate(29, 2, 2000)).isTrue();
	}

	@Test
	public void shouldReturnFalseForInvalidLeapDayInNonLeapYear() {
		assertThat(AppUtils.isValidDate(29, 2, 2001)).isFalse();
	}

	@Test
	public void shouldReturnFalseForInvalidDateInMonth() {
		assertThat(AppUtils.isValidDate(31, 9, 2020)).isFalse();
	}

	@Test
	public void shouldReturnFalseForDayZero() {
		assertThat(AppUtils.isValidDate(0, 9, 2000)).isFalse();
	}

	@Test
	public void shouldReturnFalseForDayAboveMax() {
		assertThat(AppUtils.isValidDate(32, 10, 2000)).isFalse();
	}

	@Test
	public void shouldReturnFalseForMonthAboveMax() {
		assertThat(AppUtils.isValidDate(30, 13, 2000)).isFalse();
	}

	@Test
	public void shouldReturnFalseForMonthZero() {
		assertThat(AppUtils.isValidDate(30, 0, 2000)).isFalse();
	}

	@Test
	public void shouldReturnFalseForLeapDayInYear1900_NotLeapYear() {
		assertThat(AppUtils.isValidDate(29, 2, 1900)).isFalse();
	}

	@Test
	public void shouldValidateMinYearBoundary() {
		assertThat(AppUtils.isValidDate(1, 1, 1925)).isTrue();
		assertThat(AppUtils.isValidDate(1, 1, 1924)).isFalse();
	}

	@Test
	public void shouldValidateMaxYearBoundary() {
		assertThat(AppUtils.isValidDate(31, 12, 2025)).isTrue();
		assertThat(AppUtils.isValidDate(1, 1, 2026)).isFalse();
	}

	@Test
	public void shouldReturnTrueForLeapDayIn2024() {
		assertThat(AppUtils.isValidDate(29, 2, 2024)).isTrue();
	}

	@Test
	public void shouldReturnFalseForLeapDayIn2023_NotLeapYear() {
		assertThat(AppUtils.isValidDate(29, 2, 2023)).isFalse();
	}

	@Test
	public void shouldReturnFalseForLeapDayIn2100_NotLeapYear() {
		assertThat(AppUtils.isValidDate(29, 2, 2100)).isFalse();
	}

	@Test
	public void shouldCorrectlyDetectLeapYears() {
		assertThat(AppUtils.isLeapYear(2100)).isFalse();
		assertThat(AppUtils.isLeapYear(2000)).isTrue();
		assertThat(AppUtils.isLeapYear(2024)).isTrue();
		assertThat(AppUtils.isLeapYear(1900)).isFalse();
		assertThat(AppUtils.isLeapYear(2023)).isFalse();
	}

	@Test
	public void shouldInstantiatePrivateConstructorForCoverage() throws Exception {
		Constructor<AppUtils> constructor = AppUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		AppUtils instance = constructor.newInstance();
		assertThat(instance).isNotNull().isInstanceOf(AppUtils.class);
	}

}
