package com.dyt.wcc.dytpir.ui.gallry;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dyt.wcc.dytpir.R;

import java.io.File;
import java.util.List;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2021/9/3  13:55     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.wcc.dytfourbie.main.photo     </p>
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryHolder> {
	private List<String> photoList;
	private Context mContext;
	private LayoutInflater mInflater;
	private MyOnItemClickListener onItemClickListener;

	public interface MyOnItemClickListener{
		void itemClickListener(int position );
	}

	public void setOnItemClickListener (MyOnItemClickListener itemClickListener) {
		this.onItemClickListener = itemClickListener;
	}

	public GalleryAdapter (Context context, List<String >data) {
			this.mContext = context;
			this.photoList = data;
			mInflater = LayoutInflater.from(mContext);
	}

	@NonNull
	@Override
	public GalleryHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
		return new GalleryHolder(mInflater.inflate(R.layout.item_photo_gallery, parent, false));
	}

	@Override
	public void onBindViewHolder (@NonNull GalleryHolder holder, int position) {
		if (photoList.get(position).endsWith("mp4")){
			Glide.with(mContext).load(Uri.fromFile(new File(photoList.get(position)))).into(holder.iv_item_photo);
			holder.tv_item_photo.setTextColor(mContext.getResources().getColor(R.color.red));
			holder.tv_item_photo.setText("mp4");
		}else {
			Glide.with(mContext).load(photoList.get(position)).into(holder.iv_item_photo);
		}

		holder.cl_item_container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				onItemClickListener.itemClickListener(position);
			}
		});
	}

	@Override
	public int getItemViewType (int position) {
		return position;
	}

	@Override
	public int getItemCount () {
		return photoList.size();
	}
	static class GalleryHolder extends RecyclerView.ViewHolder{
		final ImageView iv_item_photo;
		final TextView tv_item_photo;
		final ConstraintLayout cl_item_container;


		public GalleryHolder (@NonNull View itemView) {
			super(itemView);
			this.cl_item_container = itemView.findViewById(R.id.cl_item_container);
			this.iv_item_photo = itemView.findViewById(R.id.iv_item_photo_gallery);
			this.tv_item_photo = itemView.findViewById(R.id.tv_item_photo_gallery);
		}
	}


}
