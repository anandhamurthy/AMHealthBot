package com.amhealthbot.Adapters;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.amhealthbot.FullScreenImageActivity;
import com.amhealthbot.Models.Chat;
import com.amhealthbot.R;
import com.bumptech.glide.Glide;
import com.eyalbira.loadingdots.LoadingDots;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private List<Chat> chats;
    private static int TYPE_IMAGE = 1;
    private static int TYPE_MESSAGE = 2;
    private static int TYPE_LOADING = 3;
    private FirebaseAuth mAuth;
    private FirebaseUser mFirebaseUser;
    private String mCurrentUserId;
    private DatabaseReference mChatDatabase;

    public ChatAdapter(Context context, List<Chat>  chat) {
        mContext = context;
        chats = chat;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_IMAGE) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.single_chat_message_image, parent, false);
            return new ImageHolder(view);

        }else if(viewType == TYPE_LOADING){
            View view = LayoutInflater.from(mContext).inflate(R.layout.single_chat_loading, parent, false);
            return new LoadingHolder(view);
        }else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.single_chat_message, parent, false);
            return new MessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

        mAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mAuth.getCurrentUser();
        mCurrentUserId = mFirebaseUser.getUid();

        mChatDatabase = FirebaseDatabase.getInstance().getReference().child("chats");
        mChatDatabase.keepSynced(true);

        if (getItemViewType(position) == TYPE_IMAGE) {
            final Chat chat = chats.get(position);
            if (chat.getId()==1) {
                ((ImageHolder) holder).Layout.setPadding(350, 5, 15, 5);
                ((ImageHolder) holder).Layout.setGravity(Gravity.RIGHT);
                Glide.with(mContext).load(chat.getImage()).into(((ImageHolder) holder).Image);
                ((ImageHolder) holder).Image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(mContext, FullScreenImageActivity.class);
                        intent.putExtra("image",chat.getImage());
                        mContext.startActivity(intent);
                    }
                });
                ((ImageHolder) holder).Time.setText(chat.getTime());
                ((ImageHolder) holder).Message_Layout.setCardBackgroundColor(Color.parseColor("#ff99cc00"));
            }
        }
        else if(getItemViewType(position) == TYPE_LOADING){
            final Chat chat = chats.get(position);
            if (chat.getId()==0) {
                ((LoadingHolder) holder).Layout.setPadding(15, 5, 100, 5);
                ((LoadingHolder) holder).Layout.setGravity(Gravity.LEFT);
                ((LoadingHolder) holder).Message_Layout.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }else {
            final Chat chat = chats.get(position);
            if (chat.getId()==0) {
                ((MessageHolder) holder).Layout.setPadding(15, 5, 100, 5);
                ((MessageHolder) holder).Layout.setGravity(Gravity.LEFT);
                ((MessageHolder) holder).Message.setText(chat.getMessage());
                ((MessageHolder) holder).Time.setText(chat.getTime());
                ((MessageHolder) holder).Message_Layout.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            } else {
                ((MessageHolder) holder).Layout.setPadding(100, 5, 15, 5);
                ((MessageHolder) holder).Layout.setGravity(Gravity.RIGHT);
                ((MessageHolder) holder).Message.setText(chat.getMessage());
                ((MessageHolder) holder).Time.setText(chat.getTime());
                ((MessageHolder) holder).Message_Layout.setCardBackgroundColor(Color.parseColor("#ff99cc00"));
            }
        }

    }

    @Override
    public int getItemViewType(int position) {
        final Chat chat = chats.get(position);
        if (chat.getType().equals("image")) {
            return TYPE_IMAGE;
        }else if(chat.getType().equals("loading")){
            return TYPE_LOADING;
        }else {
            return TYPE_MESSAGE;
        }
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    public class MessageHolder extends RecyclerView.ViewHolder {

        public TextView Message, Time;
        public LinearLayout Layout;
        public CardView Message_Layout;

        public MessageHolder(View itemView) {
            super(itemView);

            Message = itemView.findViewById(R.id.message);
            Time = itemView.findViewById(R.id.time);
            Layout = itemView.findViewById(R.id.layout);
            Message_Layout = itemView.findViewById(R.id.layout_message);
        }
    }

    public class LoadingHolder extends RecyclerView.ViewHolder {

        public LoadingDots Dots;
        public LinearLayout Layout;
        public CardView Message_Layout;

        public LoadingHolder(View itemView) {
            super(itemView);

            Dots = itemView.findViewById(R.id.dots);
            Layout = itemView.findViewById(R.id.layout);
            Message_Layout = itemView.findViewById(R.id.layout_message);
        }
    }

    public class ImageHolder extends RecyclerView.ViewHolder {

        public ImageView Image;
        public TextView Time;
        public LinearLayout Layout;
        public CardView Message_Layout;

        public ImageHolder(View itemView) {
            super(itemView);

            Image = itemView.findViewById(R.id.image);
            Time = itemView.findViewById(R.id.time);
            Layout = itemView.findViewById(R.id.layout);
            Message_Layout = itemView.findViewById(R.id.layout_message);
        }
    }


}

