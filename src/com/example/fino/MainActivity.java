package com.example.fino;

import java.util.*;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity
{
    private ArrayList<String> mList = null;
    private HashMap<String, String> mHashmap = null;
    private Vector<String> mVector = null;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /* Initialize test vector, hashmap and list */
        mList = new ArrayList<String>();
        mList.add("Hello");
        mList.add("Kitty");
        mHashmap = new HashMap<String, String>();
        mHashmap.put("Lancelot","Green");
        mHashmap.put("Galahad","Red");
        mHashmap.put("Arthur","Purple");
        mVector = new Vector<String>();
        mVector.add("Swallow");
        mVector.add("Coconut");
    }
}
