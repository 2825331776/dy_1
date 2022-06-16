package com.dyt.wcc.dytpir.ui.pdfPreview;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.WindowManager;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dyt.wcc.common.base.BaseActivity;
import com.dyt.wcc.dytpir.BuildConfig;
import com.dyt.wcc.dytpir.R;
import com.dyt.wcc.dytpir.databinding.ActivityPdfBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PdfActivity extends BaseActivity<ActivityPdfBinding> {


	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//		setContentView(R.layout.activity_pdf);
	}

	@Override
	protected String getLanguageStr () {
		return "zh";
	}

	@Override
	protected int bindingLayout () {
		return R.layout.activity_pdf;
	}

	private Bitmap       bitmap_pdf;
	private List<Bitmap> bitmaps_pdf;

	private PdfRvAdapter pdfRvAdapter ;

	@Override
	protected void initView () {
		String filePath = "/storage/emulated/0/Android/data/" + BuildConfig.APPLICATION_ID +"/files/SLReadMeCN.pdf";
		File file = new File(filePath);
		//			File file = new File("/storage/emulated/0/Android/data/com.dyt.wcc.dytpir/files/SLReadMeCN.pdf");
		ParcelFileDescriptor pdfFile = null;
		try {
			pdfFile = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY); //以只读的方式打开文件
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		PdfRenderer renderer = null;
		try {
			renderer = new PdfRenderer(pdfFile);//用上面的pdfFile新建PdfRenderer对象
		} catch (IOException e) {
			e.printStackTrace();
		}

		final int pageCount = renderer.getPageCount();//获取pdf的页码数
		Bitmap[] bitmaps = new Bitmap[pageCount];//新建一个bmp数组用于存放pdf页面

		WindowManager wm = this.getWindowManager();//获取屏幕的高和宽，以决定pdf的高和宽
		float width = wm.getDefaultDisplay().getWidth();
		float height = wm.getDefaultDisplay().getHeight();

		for (int i = 0; i < pageCount; i++) {//这里用循环把pdf所有的页面都写入bitmap数组，真正使用的时候最好不要这样，
			//因为一本pdf的书会有很多页，一次性全部打开会非常消耗内存，我打开一本两百多页的书就消耗了1.8G的内存，而且打开速度很慢。
			//真正使用的时候要采用动态加载，用户看到哪页才加载附近的几页。而且最好使用多线程在后台打开。

			PdfRenderer.Page page = renderer.openPage(i);//根据i的变化打开每一页
			bitmap_pdf = Bitmap.createBitmap((int) (width), (int) (page.getHeight() * width / page.getWidth()), Bitmap.Config.ARGB_8888);//根据屏幕的高宽缩放生成bmp对象
			page.render(bitmap_pdf, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);//将pdf的内容写入bmp中

			bitmaps[i] = bitmap_pdf;//将pdf的bmp图像存放进数组中。

			// close the page
			page.close();
		}
		bitmaps_pdf = Arrays.asList(bitmaps.clone());
		// close the renderer
		renderer.close();

		pdfRvAdapter = new PdfRvAdapter(this,bitmaps_pdf);
//		ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//		mDataBinding.rvPdf.setLayoutParams(layoutParams);
		mDataBinding.rvPdf.setAdapter(pdfRvAdapter);


		RecyclerView.LayoutManager manager = new LinearLayoutManager(this ,RecyclerView.VERTICAL,false);
		mDataBinding.rvPdf.setLayoutManager(manager);

	}


}