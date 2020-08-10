package com.amhealthbot.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.amhealthbot.R;
import com.amhealthbot.Models.Welcome;

import java.util.List;

public class WelcomeAdapter extends PagerAdapter {

    Context mContext ;
    List<Welcome> mScreen;

    public WelcomeAdapter(Context mContext, List<Welcome> mList) {
        this.mContext = mContext;
        this.mScreen = mList;
    }


    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layoutScreen = inflater.inflate(R.layout.layout_single_welcome,null);

        ImageView image = layoutScreen.findViewById(R.id.image);
        TextView title = layoutScreen.findViewById(R.id.title);
        TextView description = layoutScreen.findViewById(R.id.description);

        title.setText(mScreen.get(position).getTitle());
        description.setText(mScreen.get(position).getDescription());
        image.setImageResource(mScreen.get(position).getScreenImg());

        container.addView(layoutScreen);

        return layoutScreen;
    }

    @Override
    public int getCount() {
        return mScreen.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view == o;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {

        container.removeView((View)object);

    }
}
