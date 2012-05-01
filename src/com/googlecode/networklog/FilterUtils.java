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
