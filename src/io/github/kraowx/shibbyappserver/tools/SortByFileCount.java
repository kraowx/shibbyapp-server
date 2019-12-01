package io.github.kraowx.shibbyappserver.tools;

import java.util.Comparator;

import io.github.kraowx.shibbyappserver.models.ShibbyFileArray;

public class SortByFileCount implements Comparator<ShibbyFileArray>
{
	public int compare(ShibbyFileArray a, ShibbyFileArray b) 
    { 
        return b.getFileCount() - a.getFileCount();
    } 
}
