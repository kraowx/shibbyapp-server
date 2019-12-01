package io.github.kraowx.shibbydexserver.tools;

import java.util.Comparator;

import io.github.kraowx.shibbydexserver.models.ShibbyFileArray;

public class SortByFileCount implements Comparator<ShibbyFileArray>
{
	public int compare(ShibbyFileArray a, ShibbyFileArray b) 
    { 
        return b.getFileCount() - a.getFileCount();
    } 
}
