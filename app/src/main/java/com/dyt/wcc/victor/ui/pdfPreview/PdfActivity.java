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

//	@Override
//	protected void onCreate (Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_pdf);
//	}

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
//	private PDFViewPager pdfViewPager;

	@Override
	protected void onDestroy () {
		super.onDestroy();
		((PDFPagerAdapter) mDataBinding.pdfViewPager.getAdapter()).close();
	}

	final String[] sampleAssets = {"SLReadMeCN.pdf", "SLReadMeEN.pdf"};
	File pdfFolder;

	protected void copyAssetsOnSDCard() {
//		final Context context = this;
		CopyAsset copyAsset = new CopyAssetThreadImpl(getApplicationContext(), new Handler(), new CopyAsset.Listener() {
			@Override
			public void success(String assetName, String destinationPath) {
				mDataBinding.pdfViewPager.setPdfPath(getPdfPathOnSDCard());
//				mDataBinding.mRootView.addView(pdfViewPager,1,null);
//				mDataBinding.pdfViewPager = pdfViewPager;
//				setContentView(pdfViewPager);
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
		mDataBinding.tvTitlePdf.setText(fileName);

//		String filePath = "/storage/emulated/0/Android/data/" + BuildConfig.APPLICATION_ID + "/files/" + fileName;

//		CopyAsset copyAsset = new CopyAssetThreadImpl(mContext.get(), new Handler());
//		copyAsset.copy(asset, new File(getCacheDir(), "sample.pdf").getAbsolutePath());
//		pdfViewPager = new PDFViewPager(this, fileName);

//		CopyAsset copyAsset = new CopyAssetThreadImpl(context, new Handler());
//		copyAsset.copy(asset, new File(getCacheDir(), "sample.pdf").getAbsolutePath());
//		PDFBoxResourceLoader.init(getApplicationContext());
		/*================================官方文档打开方式================================*/
//		File file = new File(filePath);
//		ParcelFileDescriptor pdfFile = null;
//		try {
//			pdfFile = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY); //以只读的方式打开文件
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//
//		PdfRenderer renderer = null;
//		try {
//			renderer = new PdfRenderer(pdfFile);//用上面的pdfFile新建PdfRenderer对象
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		final int pageCount = renderer.getPageCount();//获取pdf的页码数
//		Bitmap[] bitmaps = new Bitmap[pageCount];//新建一个bmp数组用于存放pdf页面
//
//		WindowManager wm = this.getWindowManager();//获取屏幕的高和宽，以决定pdf的高和宽
//		float width = wm.getDefaultDisplay().getWidth();
//		float height = wm.getDefaultDisplay().getHeight();
//
//		for (int i = 0; i < pageCount; i++) {//这里用循环把pdf所有的页面都写入bitmap数组，真正使用的时候最好不要这样，
//			//因为一本pdf的书会有很多页，一次性全部打开会非常消耗内存，我打开一本两百多页的书就消耗了1.8G的内存，而且打开速度很慢。
//			//真正使用的时候要采用动态加载，用户看到哪页才加载附近的几页。而且最好使用多线程在后台打开。
//
//			PdfRenderer.Page page = renderer.openPage(i);//根据i的变化打开每一页
//			bitmap_pdf = Bitmap.createBitmap((int) (width), (int) (page.getHeight() * width / page.getWidth()), Bitmap.Config.ARGB_8888);//根据屏幕的高宽缩放生成bmp对象
//			page.render(bitmap_pdf, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);//将pdf的内容写入bmp中
//
//			bitmaps[i] = bitmap_pdf;//将pdf的bmp图像存放进数组中。
//
//			// close the page
//			page.close();
//		}
//		bitmaps_pdf = Arrays.asList(bitmaps.clone());
//		// close the renderer
//		renderer.close();
//
//		pdfRvAdapter = new PdfRvAdapter(this, bitmaps_pdf);
//		//		ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//		//		mDataBinding.rvPdf.setLayoutParams(layoutParams);
//		mDataBinding.rvPdf.setAdapter(pdfRvAdapter);
//
//
//		RecyclerView.LayoutManager manager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
//		mDataBinding.rvPdf.setLayoutManager(manager);

		/*================================三方库打开方式=======================================*/
	}
}