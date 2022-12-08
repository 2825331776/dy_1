package com.dyt.wcc.customize.votin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.viewpager.widget.ViewPager;

import com.dyt.wcc.R;
import com.dyt.wcc.common.base.BaseActivity;
import com.dyt.wcc.constans.DYConstants;
import com.dyt.wcc.databinding.ActivityPdfBinding;

import java.io.File;
import java.util.Locale;

import es.voghdev.pdfviewpager.library.adapter.PDFPagerAdapter;
import es.voghdev.pdfviewpager.library.asset.CopyAsset;
import es.voghdev.pdfviewpager.library.asset.CopyAssetThreadImpl;


public class PdfActivity extends BaseActivity<ActivityPdfBinding> {

	private SharedPreferences sp;

	@Override
	protected String getLanguageStr () {
		if (sp != null) {
			return sp.getString(DYConstants.LANGUAGE_SETTING, Locale.getDefault().getLanguage());
		} else {
			return Locale.getDefault().getLanguage();
		}
	}

	@Override
	protected int bindingLayout () {
		return R.layout.activity_pdf;
	}

	private String fileName = "";

	@Override
	protected void onDestroy () {
		super.onDestroy();
		((PDFPagerAdapter) mDataBinding.pdfViewPager.getAdapter()).close();
	}

	final String[] sampleAssets = {"VOTINReadmeCNV2.pdf", "VOTINReadmeENV2.pdf"};
	File pdfFolder;

	private int pdfCurrentIndex = 0;

	protected void copyAssetsOnSDCard () {
		CopyAsset copyAsset = new CopyAssetThreadImpl(getApplicationContext(), new Handler(), new CopyAsset.Listener() {
			@Override
			public void success (String assetName, String destinationPath) {
				mDataBinding.pdfViewPager.setPdfPath(getPdfPathOnSDCard());

				mDataBinding.ivLeft.setVisibility(View.GONE);
				mDataBinding.pdfViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
					@Override
					public void onPageScrolled (int position, float positionOffset, int positionOffsetPixels) {
					}

					@Override
					public void onPageSelected (int position) {
						pdfCurrentIndex = position;
						if (position == 0) {
							mDataBinding.ivLeft.setVisibility(View.GONE);
							mDataBinding.ivRight.setVisibility(View.VISIBLE);
						} else if (position == mDataBinding.pdfViewPager.getAdapter().getCount() - 1) {
							mDataBinding.ivLeft.setVisibility(View.VISIBLE);
							mDataBinding.ivRight.setVisibility(View.GONE);
						} else {
							mDataBinding.ivLeft.setVisibility(View.VISIBLE);
							mDataBinding.ivRight.setVisibility(View.VISIBLE);
						}
					}

					@Override
					public void onPageScrollStateChanged (int state) {

					}
				});
				//				PDFPagerAdapter pdfPagerAdapter = (PDFPagerAdapter)mDataBinding.pdfViewPager.getAdapter();
				mDataBinding.ivLeft.setOnClickListener(v -> {
					if (pdfCurrentIndex > 0) {
						mDataBinding.pdfViewPager.setCurrentItem(pdfCurrentIndex - 1);
					}
				});
				mDataBinding.ivRight.setOnClickListener(v -> {
					if (pdfCurrentIndex < mDataBinding.pdfViewPager.getAdapter().getCount()) {
						mDataBinding.pdfViewPager.setCurrentItem(pdfCurrentIndex + 1);
					}
				});
			}

			@Override
			public void failure (Exception e) {
				e.printStackTrace();
				Toast.makeText(mContext.get(), e.getMessage(), Toast.LENGTH_LONG).show();
			}
		});

		for (String asset : sampleAssets) {
			copyAsset.copy(asset, new File(pdfFolder, asset).getAbsolutePath());
		}
	}

	protected String getPdfPathOnSDCard () {
		fileName = "VOTINReadmeCNV2.pdf";
		sp = mContext.get().getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE);
		if (sp.getInt(DYConstants.LANGUAGE_SETTING_INDEX, 0) == 1) {
			fileName = "VOTINReadmeENV2.pdf";
		}
//		mDataBinding.tvTitlePdf.setText(R.string.pdf_title);
		File f = new File(pdfFolder, fileName);
		return f.getAbsolutePath();
	}

	@Override
	protected void initView () {
		pdfFolder = Environment.getExternalStorageDirectory();
		copyAssetsOnSDCard();

		mDataBinding.ivPreviewBackPdf.setOnClickListener(v -> {
			finish();
		});

	}
}