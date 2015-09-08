/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2015 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.PwnFilter.util.tags;

import com.pwn9.PwnFilter.FilterState;

/**
 * Return the current (edited) Message text
 * Created by ptoal on 15-09-04.
 */
public class StringTag implements Tag {

    @Override
    public java.lang.String getValue(FilterState state) {
        return state.getModifiedMessage().getColoredString();
    }
}
