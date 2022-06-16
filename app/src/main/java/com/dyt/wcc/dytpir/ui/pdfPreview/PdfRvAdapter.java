package com.dyt.wcc.dytpir.ui.pdfPreview;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dyt.wcc.dytpir.R;

import java.util.List;

/**
 * <p>Copyright (C), 2018.08.08-?       </p>
 * <p>Author：stefan cheng        </p>
 * <p>Create Date：2022/6/16  10:49     </p>
 * <p>Description：@todo         </p>
 * <p>PackagePath: com.dyt.wcc.dytpir.ui.pdfPreview     </p>
 */
public class PdfRvAdapter extends RecyclerView.Adapter<PdfRvAdapter.PdfPreviewHolder> {

	private List<Bitmap>   pdfResList;
	private Context        mContext;
	private LayoutInflater mInflater;

	public PdfRvAdapter (Context context, List<Bitmap> data) {
		this.mContext = context;
		this.pdfResList = data;
		mInflater = LayoutInflater.from(mContext);
	}

	@NonNull
	@Override
	public PdfPreviewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
		return new PdfPreviewHolder(mInflater.inflate(R.layout.item_pdf_preview, parent, false));
	}

	@Override
	public void onBindViewHolder (@NonNull PdfPreviewHolder holder, int position) {
		Glide.with(mContext).load(pdfResList.get(holder.getAdapterPosition())).into(holder.iv_item_pdf_preview);
	}

	@Override
	public int getItemCount () {
		if (pdfResList != null) {
			return pdfResList.size();
		} else {
			return 0;
		}
	}

	static class PdfPreviewHolder extends RecyclerView.ViewHolder {
		final ImageView iv_item_pdf_preview;


		public PdfPreviewHolder (@NonNull View itemView) {
			super(itemView);
			this.iv_item_pdf_preview = itemView.findViewById(R.id.iv_item_pdf_preview);
		}
	}
}
