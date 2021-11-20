package com.example.chatapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.chatapp.R;
import com.example.chatapp.utilities.PreferenceManager;

public class Onboarding extends AppCompatActivity {

    private TextView tvNext;
    private ViewPager viewPager;
    private LinearLayout layout_dots;
    private PreferenceManager preferenceManager;
    private int[] layouts;
    private TextView []dots;
    private MyViewPager myViewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        preferenceManager = new PreferenceManager(this);

        if(preferenceManager.isFirstTimeLaunch()) {
            launchSignInScreen();
            finish();
        }

        tvNext = findViewById(R.id.tvNext);
        viewPager = findViewById(R.id.viewPager);
        layout_dots = findViewById(R.id.pageDots);

        layouts = new int[] {
                R.layout.intro_1,
                R.layout.intro_2,
                R.layout.intro_3
        };

        tvNext.setOnClickListener(v -> {
            int curr = getItem(+1);
            if(curr<layouts.length) {
                viewPager.setCurrentItem(curr);
            }
            else {
                launchSignInScreen();
            }
        });

        myViewPagerAdapter = new MyViewPager();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(onPageChangeListener);

        addBottomDots(0);

    }

    ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);
            if(position==layouts.length-1) {
                tvNext.setText("Start");
            }
            else {
                tvNext.setText("Next");
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }

    };

    private void addBottomDots(int currentPage) {
        dots = new TextView[layouts.length];
        int []activeColors = getResources().getIntArray(R.array.active);
        int []inactiveColors = getResources().getIntArray(R.array.inactive);
        layout_dots.removeAllViews();

        for(int i=0;i<dots.length;i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226"));
            dots[i].setTextSize(40);
            dots[i].setTextColor(inactiveColors[currentPage]);
            layout_dots.addView(dots[i]);
        }

        if(dots.length>0) {
            dots[currentPage].setTextColor(activeColors[currentPage]);
        }

    }

    public class MyViewPager extends PagerAdapter {

        LayoutInflater layoutInflater;

        public MyViewPager() {

        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(layouts[position], container, false);
            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view==object;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }

    private int getItem(int i) {
        return viewPager.getCurrentItem()+1;
    }

    private void launchSignInScreen() {
        preferenceManager.setIsfirstTimeLaunch(true);
        startActivity(new Intent(Onboarding.this,Splash_Screen.class));
        finish();
    }
}