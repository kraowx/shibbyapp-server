package io.github.kraowx.shibbyappserver.tools;

import java.util.Comparator;

import io.github.kraowx.shibbyappserver.models.ShibbyTag;

public class SortByFileCount implements Comparator<ShibbyTag>
{
	public int compare(ShibbyTag a, ShibbyTag b) 
    { 
        return b.getFileCount() - a.getFileCount();
    }
}
