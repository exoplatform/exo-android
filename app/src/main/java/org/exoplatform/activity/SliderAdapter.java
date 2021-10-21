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

import java.util.Locale;

public class SliderAdapter extends PagerAdapter {

    Context context;
    LayoutInflater layoutInflater;
    private ViewGroup container;
    private int position;

    public SliderAdapter(Context context){
        this.context = context;
    };

    // Arrays

    public static int[] slide_images = {
            Locale.getDefault().getLanguage().equals("en") ? R.drawable.slide1_gif_en : R.drawable.slide1_gif_fr,
            Locale.getDefault().getLanguage().equals("en") ? R.drawable.slide1_gif_en : R.drawable.slide1_gif_fr,
            Locale.getDefault().getLanguage().equals("en") ? R.drawable.slide1_gif_en : R.drawable.slide1_gif_fr
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
      int item = slide_images[position];
      layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View view = layoutInflater.inflate(R.layout.slides_layout, container, false);
      ImageView sliderImageView = (ImageView) view.findViewById(R.id.slider_image);
      Glide.with(context).load(item).into(sliderImageView);
      container.addView(view);
      return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((ConstraintLayout) object);
    }
}
