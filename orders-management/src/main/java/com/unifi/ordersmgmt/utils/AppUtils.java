package com.unifi.ordersmgmt.utils;

public final class AppUtils {
	private AppUtils() {
	}

	public static boolean isValidDate(int day, int month, int year) {
		// anno bisestile se divisibile per 4 non per 100 ma divisibile per 400
		boolean isLeap = isLeapYear(year);
		int maxDay;

		switch (month) {
		case 2:
			maxDay = isLeap ? 29 : 28;
			break;
		case 4:
		case 6:
		case 9:
		case 11:
			maxDay = 30;
			break;
		default:
			maxDay = 31;
		}
		return year >= (2025 - 100) && year <= 2025 && month >= 1 && month <= 12 && (day >= 1 && day <= maxDay);
	}

	public static boolean isLeapYear(int year) {
		return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
	}
}
