package com.dyt.wcc.dytpir.ui.gallery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dyt.wcc.dytpir.R;

import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/3  13:55     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.wcc.dytfourbie.main.photo     </p>
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryHolder> {
	private static final String                            TAG = "GalleryAdapter";
	private              CopyOnWriteArrayList<GalleryBean> photoList;
	private              Context                           mContext;
	private              LayoutInflater                    mInflater;
	private              MyOnItemClickListener             onItemClickListener;

	public interface MyOnItemClickListener {
		void itemClickListener (int position);
	}

	public void setOnItemClickListener (MyOnItemClickListener itemClickListener) {
		this.onItemClickListener = itemClickListener;
	}

	public GalleryAdapter (Context context, CopyOnWriteArrayList<GalleryBean> data) {
		this.mContext = context;
		this.photoList = data;
		mInflater = LayoutInflater.from(mContext);
	}

	@NonNull
	@Override
	public GalleryHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
		return new GalleryHolder(mInflater.inflate(R.layout.item_photo_gallery, parent, false));
	}

	public static String stringForTime (int timeMs) {
		if (timeMs <= 0 || timeMs >= 24 * 60 * 60 * 1000) {
			return "00:00";
		}
		int totalSeconds = timeMs / 1000;
		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;
		StringBuilder stringBuilder = new StringBuilder();
		Formatter mFormatter = new Formatter(stringBuilder, Locale.getDefault());
		if (hours > 0) {
			return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
		} else {
			return mFormatter.format("%02d:%02d", minutes, seconds).toString();
		}
	}

	@Override
	public void onBindViewHolder (@NonNull GalleryHolder holder, int position) {
		if (photoList.get(position).getType() == 0) {
			holder.ivItemPlayVideo.setVisibility(View.GONE);
		} else {
			holder.ivItemPlayVideo.setVisibility(View.VISIBLE);
			holder.tvItemVideoLength.setVisibility(View.VISIBLE);
			holder.tvItemVideoLength.setTextColor(mContext.getResources().getColor(R.color.white));
			holder.tvItemVideoLength.setText(stringForTime(photoList.get(position).getVideoDuration()));
		}
		holder.ivItemCheck.setSelected(photoList.get(position).isSelect());
		Glide.with(mContext).load(photoList.get(position).getUriAddress()).centerCrop().into(holder.ivItemPhoto);
		holder.ll_main_gallery_item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				photoList.get(position).setSelect(!photoList.get(position).isSelect());
				holder.ivItemCheck.setSelected(photoList.get(position).isSelect());
			}
		});
		holder.cl_item_container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				onItemClickListener.itemClickListener(position);
			}
		});
	}

	@Override
	public int getItemViewType (int position) {
		return photoList.get(position).getType();
	}

	@Override
	public int getItemCount () {
		return photoList.size();
	}

	static class GalleryHolder extends RecyclerView.ViewHolder {
		final ImageView        ivItemPhoto;
		final ImageView        ivItemCheck;
		final ImageView        ivItemPlayVideo;
		final TextView         tvItemVideoLength;
		final ConstraintLayout cl_item_container;//整个item 父布局
		final LinearLayout     ll_main_gallery_item;//选中框的父布局


		public GalleryHolder (@NonNull View itemView) {
			super(itemView);
			this.cl_item_container = itemView.findViewById(R.id.cl_item_container);
			this.ivItemPhoto = itemView.findViewById(R.id.iv_main_gallery_item_photo_gallery);
			this.ivItemCheck = itemView.findViewById(R.id.iv_main_gallery_item_check);
			this.ivItemPlayVideo = itemView.findViewById(R.id.iv_main_gallery_item_playVideo);
			this.tvItemVideoLength = itemView.findViewById(R.id.tv_main_gallery_item_video_length);
			this.ll_main_gallery_item = itemView.findViewById(R.id.ll_main_gallery_item_check);
		}
	}
}
