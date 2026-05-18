package com.reprush.app.utils

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.reprush.app.R

object AssetImageLoader {

    fun load(context: Context, assetPath: String?, imageView: ImageView) {
        if (assetPath.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_exercise_placeholder)
            return
        }
        Glide.with(context)
            .load(Uri.parse("file:///android_asset/$assetPath"))
            .placeholder(R.drawable.ic_exercise_placeholder)
            .error(R.drawable.ic_exercise_placeholder)
            .into(imageView)
    }

    fun loadThumbnail(context: Context, assetPath: String?, imageView: ImageView) {
        if (assetPath.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_exercise_placeholder)
            return
        }
        Glide.with(context)
            .load(Uri.parse("file:///android_asset/$assetPath"))
            .thumbnail(0.25f)
            .placeholder(R.drawable.ic_exercise_placeholder)
            .error(R.drawable.ic_exercise_placeholder)
            .into(imageView)
    }
}
