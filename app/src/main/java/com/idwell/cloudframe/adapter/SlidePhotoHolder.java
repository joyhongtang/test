package com.idwell.cloudframe.adapter;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class SlidePhotoHolder extends RecyclerView.ViewHolder {
    public ImageView imageView;

    public SlidePhotoHolder(@NonNull View view) {
        super(view);
        this.imageView = (ImageView) view;
    }
}