package com.dyt.wcc.customize.mileseey;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.viewpager.widget.ViewPager;

import com.dyt.wcc.R;
import com.dyt.wcc.constans.DYConstants;
import com.dyt.wcc.databinding.ActivityPdfBinding;

import java.io.File;

import es.voghdev.pdfviewpager.library.adapter.PDFPagerAdapter;


public class PdfActivity extends/* BaseActivity<ActivityPdfBinding>*/ AppCompatActivity {

	private SharedPreferences sp;

/*	@Override
	protected String getLanguageStr () {
		if (sp != null) {
			return sp.getString(DYConstants.LANGUAGE_SETTING, "en-rUS").split("-r")[0];
		} else {
			return "en";
		}
	}*/

	/*	@Override
		protected int bindingLayout () {
			return R.layout.activity_pdf;
		}*/
	private ActivityPdfBinding mDataBinding;

	@Override
	protected void onCreate (@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_pdf);


		initView();
	}

	private String fileName = "";

	@Override
	protected void onDestroy () {
		super.onDestroy();
		((PDFPagerAdapter) mDataBinding.pdfViewPager.getAdapter()).close();
	}

//	final String[] sampleAssets = {"TRReadmeCN.pdf", "TRReadmeEN.pdf", "TRReadmeDE.pdf"};
	File pdfFolder;

	private int pdfCurrentIndex = 0;

	protected void copyAssetsOnSDCard () {
//		CopyAsset copyAsset = new CopyAssetThreadImpl(getApplicationContext(), new Handler(), new CopyAsset.Listener() {
//			@Override
//			public void success (String assetName, String destinationPath) {
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
//			}

//			@Override
//			public void failure (Exception e) {
//				e.printStackTrace();
//				Toast.makeText(PdfActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
//			}
//		});

//		for (String asset : sampleAssets) {
//			copyAsset.copy(asset, new File(pdfFolder, asset).getAbsolutePath());
//		}
	}

	protected String getPdfPathOnSDCard () {
		String langStr = sp.getString(DYConstants.LANGUAGE_SETTING, "");
		if (langStr.contains("zh")) {
			fileName = "TRReadmeCN.pdf";
		} else if (langStr.contains("en")) {
			fileName = "TRReadmeEN.pdf";
		} else if (langStr.contains("de")) {
			fileName = "TRReadmeDE.pdf";
		} else {
			fileName = "TRReadmeEN.pdf";
		}
//		mDataBinding.tvTitlePdf.setText(fileName);
		File f = new File(pdfFolder, fileName);
		return f.getAbsolutePath();
	}

	protected void initView () {
		sp = this.getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE);

//		pdfFolder = Environment.getExternalStorageDirectory();
		pdfFolder = this.getExternalFilesDir(null);
//		Log.e("----mileSeey--", "initView: ----------pdfFolder---+" + pdfFolder);
		copyAssetsOnSDCard();

		mDataBinding.ivPreviewBackPdf.setOnClickListener(v -> {
			finish();
		});

	}
}