package org.exoplatform.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;

import org.exoplatform.R;

public class SliderAdapter extends PagerAdapter {

    Context context;
    LayoutInflater layoutInflater;
    private ViewGroup container;
    private int position;

    public SliderAdapter(Context context){
        this.context = context;
    }

    // Arrays
    public static int[] slide_images = {
            R.drawable.slide1_gif_en,
            R.drawable.slide2_gif_en,
            R.drawable.slide3_gif_en
    };

    @Override
    public int getCount() {
        return slide_images.length ;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (ConstraintLayout) object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
      layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View view = layoutInflater.inflate(R.layout.slides_layout, container, false);
      ImageView sliderImageView = (ImageView) view.findViewById(R.id.slider_image);
      Glide.with(context).load(slide_images[position]).into(sliderImageView);
      container.addView(view);
      return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((ConstraintLayout) object);
    }
}
