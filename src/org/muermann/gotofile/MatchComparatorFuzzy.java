/*
    GotoFile Eclipse Plugin - Quicksearch for files in Eclipse IDE
    Copyright (C) 2004 Max Muermann

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.muermann.gotofile;

import java.util.Comparator;

/**
 * @author max
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MatchComparatorFuzzy implements Comparator
{
	private static MatchComparatorFuzzy instance = new MatchComparatorFuzzy();

	public static MatchComparatorFuzzy getInstance()
	{
		return instance;
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object o1, Object o2)
	{
		SearchResult r1 = (SearchResult)o1;
		SearchResult r2 = (SearchResult)o2;
		String r1Name = r1.getFile().getName();
		String r2Name = r2.getFile().getName();

		if (r1.isOpen() && !r2.isOpen())
			return -1;
		if (r2.isOpen() && !r1.isOpen())
			return 1;

		if (r1.isCapsMatched() && !r2.isCapsMatched())
			return -1;
		if (r2.isCapsMatched() && !r1.isCapsMatched())
			return 1;

		if (r1.getMatchConsecutive() > r2.getMatchConsecutive())
			return -1;
		if (r2.getMatchConsecutive() > r1.getMatchConsecutive())
			return 1;

		if (r1Name.contains("Test.") && !r2Name.contains("Test."))
			return 1;
		if (r2Name.contains("Test.") && !r1Name.contains("Test."))
			return -1;

		if (r1Name.length() < r2Name.length())
			return -1;
		if (r2Name.length() < r1Name.length())
			return 1;

		return r2.getMatchPos() - r1.getMatchPos();
	}
}
