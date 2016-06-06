/*
 *  PwnFilter - Chat and user-input filter with the power of Regex
 *  Copyright (C) 2016 Pwn9.com / Sage905 <sage905@takeflight.ca>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.pwn9.filter.engine.rules.parser;

/**
 * <p>NumberedLine class.</p>
 *
 * @author Sage905
 * @version $Id: $Id
 */
class NumberedLine {
    public final Integer number;
    public final String string;

    /**
     * <p>Constructor for NumberedLine.</p>
     *
     * @param number a {@link java.lang.Integer} object.
     * @param string a {@link java.lang.String} object.
     */
    NumberedLine(Integer number, String string) {
        this.number = number;
        this.string = string;
    }
}