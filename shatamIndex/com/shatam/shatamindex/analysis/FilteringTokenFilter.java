/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.analysis;

import java.io.IOException;

import com.shatam.shatamindex.analysis.tokenattributes.PositionIncrementAttribute;

public abstract class FilteringTokenFilter extends TokenFilter {

	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	private boolean enablePositionIncrements;

	public FilteringTokenFilter(boolean enablePositionIncrements,
			TokenStream input) {
		super(input);
		this.enablePositionIncrements = enablePositionIncrements;
	}

	protected abstract boolean accept() throws IOException;

	@Override
	public final boolean incrementToken() throws IOException {
		if (enablePositionIncrements) {
			int skippedPositions = 0;
			while (input.incrementToken()) {
				if (accept()) {
					if (skippedPositions != 0) {
						posIncrAtt.setPositionIncrement(posIncrAtt
								.getPositionIncrement() + skippedPositions);
					}
					return true;
				}
				skippedPositions += posIncrAtt.getPositionIncrement();
			}
		} else {
			while (input.incrementToken()) {
				if (accept()) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean getEnablePositionIncrements() {
		return enablePositionIncrements;
	}

	public void setEnablePositionIncrements(boolean enable) {
		this.enablePositionIncrements = enable;
	}
}
