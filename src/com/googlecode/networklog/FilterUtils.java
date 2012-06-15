/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

import java.util.ArrayList;

public class FilterUtils
{
  public static void buildList(String filter, ArrayList<String> list)
  {
    if(filter.length() > 0)
    {
      list.clear();

      String[] keywords = filter.split(",");

      for(String keyword : keywords)
      {
        keyword = keyword.trim().toLowerCase();
        list.add(keyword);
      }
    }
  }
}
