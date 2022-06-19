package com.dyt.wcc.victor.ui.pdfPreview;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import com.dyt.wcc.common.base.BaseActivity;
import com.dyt.wcc.victor.R;
import com.dyt.wcc.victor.constans.DYConstants;
import com.dyt.wcc.victor.databinding.ActivityPdfBinding;

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

	private String       fileName = "";

	@Override
	protected void onDestroy () {
		super.onDestroy();
		((PDFPagerAdapter) mDataBinding.pdfViewPager.getAdapter()).close();
	}

	final String[] sampleAssets = {"SLReadMeCN.pdf", "SLReadMeEN.pdf"};
	File pdfFolder;

	protected void copyAssetsOnSDCard() {
		CopyAsset copyAsset = new CopyAssetThreadImpl(getApplicationContext(), new Handler(), new CopyAsset.Listener() {
			@Override
			public void success(String assetName, String destinationPath) {
				mDataBinding.pdfViewPager.setPdfPath(getPdfPathOnSDCard());
			}

			@Override
			public void failure(Exception e) {
				e.printStackTrace();
				Toast.makeText(mContext.get(), e.getMessage(), Toast.LENGTH_LONG).show();
			}
		});

		for (String asset : sampleAssets) {
			copyAsset.copy(asset, new File(pdfFolder, asset).getAbsolutePath());
		}
	}

	protected String getPdfPathOnSDCard() {
		fileName = "SLReadMeCN.pdf";
		sp = mContext.get().getSharedPreferences(DYConstants.SP_NAME, Context.MODE_PRIVATE);
		if (sp.getInt(DYConstants.LANGUAGE_SETTING_INDEX, 0) == 1) {
			fileName = "SLReadMeEN.pdf";
		}
		mDataBinding.tvTitlePdf.setText(fileName);
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