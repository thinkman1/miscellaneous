package com.free.commons.util;

/**
 * provide some classes like StringUtils in commons lang for
 * StringBuilder/CharSequence objects to make buffer re-use easier
 * 
 * @author E001668
 */
public class StringBuilderUtils {
	/**
	 * the index of method for abstract string builders & strings wants a char
	 * array, and that means that many of the CharSequence objects have to copy
	 * their data into a new array. try to save some allocations, try to use
	 * CharSequence.charAt() which is a really fast call for those same objects.
	 * 
	 * @param source
	 *            the characters being searched.
	 * @param sourceOffset
	 *            offset of the source string.
	 * @param sourceCount
	 *            count of the source string.
	 * @param target
	 *            the characters being searched for.
	 * @param targetOffset
	 *            offset of the target string.
	 * @param targetCount
	 *            count of the target string.
	 * @param fromIndex
	 *            the index to begin searching from.
	 * @return
	 */
	static int indexOf(CharSequence source, CharSequence target, int fromIndex) {
		if (source.length() == 0) {
			return -1;
		}

		if (target.length() == 0) {
			return -1;
		}

		if (target.length() > source.length()) {
			return -1;
		}

		for (int i = fromIndex; i < source.length(); i++) {
			int firstMatch = 0;
			if (source.charAt(i) == target.charAt(0)) {
				firstMatch = i;
				int j = 0;
				for (j = 0; j < target.length(); j++) {
					if (target.charAt(j) != source.charAt(i)) {
						// go backwards and re-try
						i = i - j;
						break;
					}
					i++;
				}
				if (j == target.length()) {
					return firstMatch;
				}
			}
		}
		return -1;
	}

	public static void trim(StringBuilder builder) {
		if (builder == null) {
			throw new NullPointerException();
		}
		while ((builder.length() > 0)
				&& (Character.isWhitespace(builder.charAt(0)))) {
			builder.deleteCharAt(0);
		}
		while ((builder.length() > 0)
				&& (Character
						.isWhitespace(builder.charAt(builder.length() - 1)))) {
			builder.deleteCharAt(builder.length() - 1);
		}
	}

	public static int lastIndexOf(CharSequence input, CharSequence checkFor) {
		int ret = -1;
		int check = 0;
		while (check > -1) {
			check = indexOf(input, checkFor, check);
			if (check > -1) {
				ret = check;
			} else {
				break;
			}
		}
		return ret;
	}

	public static void removeString(StringBuilder builder, String data) {
		if (data == null) {
			return;
		}
		int index = indexOf(builder, data, 0);
		while (index > -1) {
			builder.delete(index, index + data.length());
			if (index + data.length() > builder.length()) {
				return;
			}
			index = indexOf(builder, data, 0);
		}
	}

	public static void replaceString(StringBuilder builder, CharSequence replace,
			CharSequence replaceWith) {
		int index = indexOf(builder, replace, 0);
		while (index > -1) {
			builder.delete(index, index + replace.length());
			builder.insert(index, replaceWith);
			index = indexOf(builder, replace, index + 1);
		}
	}

	public static void toLowerCase(StringBuilder builder) {
		for (int i = 0; i < builder.length(); i++) {
			if (Character.isUpperCase(builder.charAt(i))) {
				builder.setCharAt(i, Character.toLowerCase(builder.charAt(i)));
			}
		}
	}

	public static void toUpperCase(StringBuilder builder) {
		for (int i = 0; i < builder.length(); i++) {
			if (Character.isLowerCase(builder.charAt(i))) {
				builder.setCharAt(i, Character.toUpperCase(builder.charAt(i)));
			}
		}
	}

	public static boolean startsWith(CharSequence string,
			CharSequence startsWith) {
		if ((string == null) || (startsWith == null)) {
			return false;
		}

		if (string.length() < startsWith.length()) {
			return false;
		}

		if (startsWith.length() == 0) {
			return false;
		}

		int max = startsWith.length();
		for (int i = 0; i < max; i++) {
			if (string.charAt(i) != startsWith.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	public static boolean endsWith(CharSequence string, CharSequence endsWith) {
		if ((string == null) || (endsWith == null)) {
			return false;
		}

		if (string.length() < endsWith.length()) {
			return false;
		}

		if (endsWith.length() == 0) {
			return false;
		}

		if (endsWith.length() == 1) {
			return string.charAt(string.length() - 1) == endsWith.charAt(0);
		}

		int max = endsWith.length();
		for (int i = 0; i < max - 1; i++) {
			int endString = string.length() - i - 1;
			int endWithString = endsWith.length() - i - 1;
			if (string.charAt(endString) != endsWith.charAt(endWithString)) {
				return false;
			}
		}
		return true;
	}

	public static boolean equalsIgnoreCase(CharSequence seq1, CharSequence seq2) {
		if ((seq1 == null) && (seq2 == null)) {
			return true;
		}

		if ((seq1 == null) || (seq2 == null)) {
			return false;
		}

		if (seq1.length() != seq2.length()) {
			return false;
		}
		for (int i = 0; i < seq1.length(); i++) {
			char one = seq1.charAt(i);
			char two = seq2.charAt(i);
			if (one != two) {
				if (Character.isLetter(one) && Character.isLetter(two)) {
					// string equalsIgnoreCase does this (both upper and lower
					// check)
					one = Character.toUpperCase(one);
					two = Character.toUpperCase(two);
					if (one != two) {
						one = Character.toLowerCase(one);
						two = Character.toLowerCase(two);
						if (one != two) {
							return false;
						}
					}
				} else {
					return false;
				}
			}
		}
		return true;
	}
}
