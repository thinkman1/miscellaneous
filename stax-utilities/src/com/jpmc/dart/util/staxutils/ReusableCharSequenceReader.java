package com.jpmc.dart.util.staxutils;

import java.io.Reader;

/**
 * a reusable reader that reads directly from a CharSequence object
 * 
 * @author e001668
 */
public class ReusableCharSequenceReader extends Reader {
	int upto;
	int left;
	private CharSequence s;

	public void init(CharSequence s) {
		this.s = s;
		left = s.length();
		this.upto = 0;
	}

	public int read(char[] c) {
		return read(c, 0, c.length);
	}

	public int read(char[] c, int off, int len) {
		if (left > len) {
			getChars(upto, upto + len, c, off);
			upto += len;
			left -= len;
			return len;
		} else if (0 == left) {
			return -1;
		} else {
			getChars(upto, upto + left, c, off);
			int r = left;
			left = 0;
			upto = s.length();
			return r;
		}
	}

	public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
		if (s instanceof String) {
			((String) s).getChars(srcBegin, srcEnd, dst, dstBegin);
		} else if (s instanceof StringBuilder) {
			((StringBuilder) s).getChars(srcBegin, srcEnd, dst, dstBegin);
		} else if (s instanceof StringBuffer) {
			((StringBuffer) s).getChars(srcBegin, srcEnd, dst, dstBegin);
		} else {
			int dstCounter = dstBegin;
			for (int i = srcBegin; i < srcEnd; i++) {
				dst[dstCounter] = s.charAt(i);
				dstCounter++;
			}
		}
	}

	public void close() {
		s = null;
	}
}
