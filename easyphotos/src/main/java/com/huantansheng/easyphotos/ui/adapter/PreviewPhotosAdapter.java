package com.huantansheng.easyphotos.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.github.chrisbanes.photoview.OnScaleChangedListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.huantansheng.easyphotos.R;
import com.huantansheng.easyphotos.constant.Type;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.huantansheng.easyphotos.setting.Setting;
import com.huantansheng.easyphotos.utils.media.DurationUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * 大图预览界面图片集合的适配器
 * Created by huan on 2017/10/26.
 */
public class PreviewPhotosAdapter extends RecyclerView.Adapter<PreviewPhotosAdapter.PreviewPhotosViewHolder> {
	private static final String           TAG = "PreviewPhotosAdapter";
	private              ArrayList<Photo> photos;
	private              OnClickListener  listener;
	private              LayoutInflater   inflater;

	public interface OnClickListener {
		void onPhotoClick ();

		void onPhotoScaleChanged ();
	}

	public PreviewPhotosAdapter (Context cxt, ArrayList<Photo> photos, OnClickListener listener) {
		this.photos = photos;
		this.inflater = LayoutInflater.from(cxt);
		this.listener = listener;
	}

	@NonNull
	@Override
	public PreviewPhotosViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
		return new PreviewPhotosViewHolder(inflater.inflate(R.layout.item_preview_photo_easy_photos, parent, false));
	}

	@Override
	public void onBindViewHolder (@NonNull final PreviewPhotosViewHolder holder, int position) {
		final Uri uri = photos.get(position).uri;
		final String path = photos.get(position).path;
		final String type = photos.get(position).type;
		final double ratio = (double) photos.get(position).height / (double) photos.get(position).width;

		holder.ivPlay.setVisibility(View.GONE);
		holder.ivPhotoView.setVisibility(View.GONE);
		holder.ivLongPhoto.setVisibility(View.GONE);

		holder.tvDuration.setVisibility(View.INVISIBLE);

		holder.tvFileName.setText(String.format("%s", photos.get(position).name));

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);
		// Create a calendar object that will convert the date and time value in milliseconds to date.
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(photos.get(position).time * 1000);
		holder.tvShootingTime.setText(String.format("%s", formatter.format(calendar.getTime())));

		if (type.contains(Type.VIDEO)) {
			//            holder.tvDuration.setText(String.format("%ss", photos.get(position).duration/1000));
			holder.tvDuration.setText(DurationUtils.format(photos.get(position).duration));
			holder.tvDuration.setVisibility(View.VISIBLE);
			holder.ivPhotoView.setVisibility(View.VISIBLE);

			//			MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
			//			mediaMetadataRetriever.setDataSource(photos.get(position).path);
			//			Bitmap frameBitmap = mediaMetadataRetriever.getFrameAtTime(1000000);
			//			mediaMetadataRetriever.release();
			//			Glide.with(holder.ivPhotoView.getContext()).load(frameBitmap).into(holder.ivPhotoView);

			Setting.imageEngine.loadPhoto(holder.ivPhotoView.getContext(), uri, holder.ivPhotoView);
			holder.ivPlay.setVisibility(View.VISIBLE);
			holder.ivPlay.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					toPlayVideo(v, uri, type);
				}
			});
		} else if (path.endsWith(Type.GIF) || type.endsWith(Type.GIF)) {
			holder.ivPhotoView.setVisibility(View.VISIBLE);
			Setting.imageEngine.loadGif(holder.ivPhotoView.getContext(), uri, holder.ivPhotoView);
		} else {
			if (ratio > 2.3) {
				holder.ivLongPhoto.setVisibility(View.VISIBLE);
				holder.ivLongPhoto.setImage(ImageSource.uri(path));
			} else {
				holder.ivPhotoView.setVisibility(View.VISIBLE);
				Setting.imageEngine.loadPhoto(holder.ivPhotoView.getContext(), uri, holder.ivPhotoView);
			}
		}

		holder.ivLongPhoto.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				listener.onPhotoClick();
			}
		});
		holder.ivPhotoView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				listener.onPhotoClick();
			}
		});
		holder.ivLongPhoto.setOnStateChangedListener(new SubsamplingScaleImageView.OnStateChangedListener() {
			@Override
			public void onScaleChanged (float newScale, int origin) {
				listener.onPhotoScaleChanged();
			}

			@Override
			public void onCenterChanged (PointF newCenter, int origin) {

			}
		});

		holder.ivPhotoView.setScale(1f);

		holder.ivPhotoView.setOnScaleChangeListener(new OnScaleChangedListener() {
			@Override
			public void onScaleChange (float scaleFactor, float focusX, float focusY) {
				listener.onPhotoScaleChanged();
			}
		});
	}

	private void toPlayVideo (View v, Uri uri, String type) {
		Context context = v.getContext();
		Intent intent = new Intent(Intent.ACTION_VIEW);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		}
		intent.setDataAndType(uri, type);
		context.startActivity(intent);
	}

	private Uri getUri (Context context, String path, Intent intent) {
		File file = new File(path);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			return FileProvider.getUriForFile(context, Setting.fileProviderAuthority, file);
		} else {
			return Uri.fromFile(file);
		}
	}

	@Override
	public int getItemCount () {
		return photos.size();
	}

	public class PreviewPhotosViewHolder extends RecyclerView.ViewHolder {
		public SubsamplingScaleImageView ivLongPhoto;
		ImageView ivPlay;
		PhotoView ivPhotoView;
		TextView  tvFileName;//文件名
		TextView  tvShootingTime;//拍摄时间
		TextView  tvDuration;//时长

		PreviewPhotosViewHolder (View itemView) {
			super(itemView);
			ivLongPhoto = itemView.findViewById(R.id.iv_long_photo);
			ivPhotoView = itemView.findViewById(R.id.iv_photo_view);
			ivPlay = itemView.findViewById(R.id.iv_play);
			tvFileName = itemView.findViewById(R.id.tv_file_name);
			tvShootingTime = itemView.findViewById(R.id.tv_shooting_time);
			tvDuration = itemView.findViewById(R.id.tv_file_duration);
		}
	}
}
