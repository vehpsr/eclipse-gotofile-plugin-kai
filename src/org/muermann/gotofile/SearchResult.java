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

import org.eclipse.core.resources.IFile;

/**
 * @author max
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SearchResult {

    private int order;

    private IFile file;
    private int matchPos;
    private int matchConsecutive;
    private boolean capsMatched;
    public boolean resourcesSorted = false;
    private boolean isOpen;
    private int matchConsecutiveName;

    public SearchResult(IFile file, int order) {
        this.file = file;
        this.order = order;
    }

    public SearchResult(IFile file, boolean isOpen, boolean capsMatched, int matchConsecutiveName, int matchConsecutive, int matchPos) {
        this.file = file;
        this.isOpen = isOpen;
        this.matchPos = matchPos;
        this.matchConsecutive = matchConsecutive;
        this.matchConsecutiveName = matchConsecutiveName;
        this.capsMatched = capsMatched;
    }

    public int getOrder() {
        return order;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public int getMatchConsecutiveName() {
        return matchConsecutiveName;
    }

    /**
     * @return
     */
    public IFile getFile()
    {
        return file;
    }

    /**
     * @return
     */
    public int getMatchConsecutive()
    {
        return matchConsecutive;
    }

    /**
     * @return
     */
    public int getMatchPos()
    {
        return matchPos;
    }

    /**
     * @param file
     */
    public void setFile(IFile file)
    {
        this.file = file;
    }

    /**
     * @param i
     */
    public void setMatchConsecutive(int i)
    {
        matchConsecutive = i;
    }

    /**
     * @param i
     */
    public void setMatchPos(int i)
    {
        matchPos = i;
    }

    /**
     * @return
     */
    public boolean isCapsMatched()
    {
        return capsMatched;
    }

    /**
     * @param b
     */
    public void setCapsMatched(boolean b)
    {
        capsMatched = b;
    }

}
