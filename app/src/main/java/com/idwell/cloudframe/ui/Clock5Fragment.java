package com.idwell.cloudframe.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.idwell.cloudframe.R;

/**
 * author : chason
 * mailbox : 156874547@qq.com
 * time : 2018/5/7 19:43
 * version : 1.0
 * describe :
 */
public class Clock5Fragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.analog_clock5, container, false);
    }
}
