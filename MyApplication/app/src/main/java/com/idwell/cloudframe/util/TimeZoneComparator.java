package com.idwell.cloudframe.util;

import java.util.Comparator;
import java.util.HashMap;

public class TimeZoneComparator implements Comparator<HashMap<?,?>> {

    private String mSortingKey;

    public TimeZoneComparator(String sortingKey) {
        mSortingKey = sortingKey;
    }

    public void setSortingKey(String sortingKey) {
        mSortingKey = sortingKey;
    }

    public int compare(HashMap<?, ?> map1, HashMap<?, ?> map2) {
        Object value1 = map1.get(mSortingKey);
        Object value2 = map2.get(mSortingKey);

        /*
         * This should never happen, but just in-case, put non-comparable
         * items at the end.
         */
        if (!isComparable(value1)) {
            return isComparable(value2) ? 1 : 0;
        } else if (!isComparable(value2)) {
            return -1;
        }

        return ((Comparable) value1).compareTo(value2);
    }

    private boolean isComparable(Object value) {
        return (value != null) && (value instanceof Comparable);
    }
}
