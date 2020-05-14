package com.example.filechange

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.Display
import android.view.WindowManager
import java.util.*

/**
 * Created by <lzh> on 2017/8/2.
</lzh> */
class ScreenShotListenManager private constructor(context: Context?) {
    private val mContext: Context
    private var mListener: OnScreenShotListener? = null
    private var mStartListenTime: Long = 0
    /**
     * 内部存储器内容观察者
     */
    private var mInternalObserver: MediaContentObserver? = null
    /**
     * 外部存储器内容观察者
     */
    private var mExternalObserver: MediaContentObserver? = null
    /**
     * 运行在 UI 线程的 Handler, 用于运行监听器回调
     */
    private val mUiHandler = Handler(Looper.getMainLooper())

    /**
     * 启动监听
     */
    fun startListen() {
        assertInMainThread()
        //        sHasCallbackPaths.clear();
// 记录开始监听的时间戳
        mStartListenTime = System.currentTimeMillis()
        // 创建内容观察者
        mInternalObserver =
            MediaContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, mUiHandler)
        mExternalObserver =
            MediaContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mUiHandler)
        // 注册内容观察者
        mContext.contentResolver.registerContentObserver(
            MediaStore.Images.Media.INTERNAL_CONTENT_URI,
            false,
            mInternalObserver!!
        )
        mContext.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            false,
            mExternalObserver!!
        )
    }

    /**
     * 停止监听
     */
    fun stopListen() {
        assertInMainThread()
        // 注销内容观察者
        if (mInternalObserver != null) {
            try {
                mContext.contentResolver.unregisterContentObserver(mInternalObserver!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mInternalObserver = null
        }
        if (mExternalObserver != null) {
            try {
                mContext.contentResolver.unregisterContentObserver(mExternalObserver!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mExternalObserver = null
        }
        // 清空数据
        mStartListenTime = 0
        //        sHasCallbackPaths.clear();
//切记！！！:必须设置为空 可能mListener 会隐式持有Activity导致释放不掉
        mListener = null
    }

    /**
     * 处理媒体数据库的内容改变
     */
    private fun handleMediaContentChange(contentUri: Uri) {
        Log.d(TAG, "handleMediaContentChange. contentUri: $contentUri")
        var cursor: Cursor? = null
        try { // 数据改变时查询数据库中最后加入的一条数据
            cursor = mContext.contentResolver.query(
                contentUri,
                MEDIA_PROJECTIONS_API_16,
                null,
                null,
                MediaStore.Images.ImageColumns.DATE_ADDED + " desc limit 1"
            )
            if (cursor == null) {
                Log.e(TAG, "Deviant logic.")
                return
            }
            if (!cursor.moveToFirst()) {
                Log.d(TAG, "Cursor no data.")
                return
            }
            // 获取各列的索引
            val dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            val dateTakenIndex =
                cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN)
            var widthIndex = -1
            var heightIndex = -1
            if (Build.VERSION.SDK_INT >= 16) {
                widthIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH)
                heightIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT)
            }
            // 获取行数据
            val filePath = cursor.getString(dataIndex)
            val dateTaken = cursor.getLong(dateTakenIndex)
            var width = 0
            var height = 0
            if (widthIndex >= 0 && heightIndex >= 0) {
                width = cursor.getInt(widthIndex)
                height = cursor.getInt(heightIndex)
            } else { // API 16 之前, 宽高要手动获取
                val size = getImageSize(filePath)
                width = size.x
                height = size.y
            }
            // 处理获取到的第一行数据
            handleMediaRowData(filePath, dateTaken, width, height)
        } catch (e: Exception) {
//            e.printStackTrace()
            Log.e(TAG, "handleMediaContentChange. catch: ${e.message}")
        } finally {
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
    }

    private fun getImageSize(imagePath: String): Point {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)
        return Point(options.outWidth, options.outHeight)
    }

    /**
     * 处理获取到的一行数据
     */
    private fun handleMediaRowData(
        filePath: String,
        dateTaken: Long,
        width: Int,
        height: Int
    ) {
        if (checkScreenShot(filePath, dateTaken, width, height)) {
            Log.d(
                TAG,
                "ScreenShot: path = " + filePath + "; size = " + width + " * " + height
                        + "; date = " + dateTaken
            )
            if (mListener != null && !checkCallback(filePath)) {
                mListener!!.onShot(filePath)
            }
        } else { // 如果在观察区间媒体数据库有数据改变，又不符合截屏规则，则输出到 log 待分析
            Log.e(
                TAG,
                "Media content changed, but not screenshot: path = " + filePath
                        + "; size = " + width + " * " + height + "; date = " + dateTaken
            )
        }
    }

    /**
     * 判断指定的数据行是否符合截屏条件
     */
    private fun checkScreenShot(
        data: String,
        dateTaken: Long,
        width: Int,
        height: Int
    ): Boolean { /*
         * 判断依据一: 时间判断
         */
// 如果加入数据库的时间在开始监听之前, 或者与当前时间相差大于10秒, 则认为当前没有截屏
        var data = data
        Log.d(TAG, "checkScreenShot. dateTaken: $dateTaken")
        Log.d(TAG, "checkScreenShot. mStartListenTime: $mStartListenTime")
        if (dateTaken < mStartListenTime || System.currentTimeMillis() - dateTaken > 10 * 1000) {
            return false
        }
        /*
         * 判断依据二: 尺寸判断
         */if (sScreenRealSize != null) { // 如果图片尺寸超出屏幕, 则认为当前没有截屏
            if (!(width <= sScreenRealSize?.x?:0 && height <= sScreenRealSize?.y?:0
                        || height <= sScreenRealSize?.x?:0 && width <= sScreenRealSize?.y?:0)
            ) {
                return false
            }
        }
        /*
         * 判断依据三: 路径判断
         */if (TextUtils.isEmpty(data)) {
            return false
        }
        data = data.toLowerCase()
        // 判断图片路径是否含有指定的关键字之一, 如果有, 则认为当前截屏了
        for (keyWork in KEYWORDS) {
            if (data.contains(keyWork)) {
                return true
            }
        }
        return false
    }

    /**
     * 判断是否已回调过, 某些手机ROM截屏一次会发出多次内容改变的通知; <br></br>
     * 删除一个图片也会发通知, 同时防止删除图片时误将上一张符合截屏规则的图片当做是当前截屏.
     */
    private fun checkCallback(imagePath: String): Boolean {
        if (sHasCallbackPaths.contains(imagePath)) {
            Log.d(
                TAG, "ScreenShot: imgPath has done"
                        + "; imagePath = " + imagePath
            )
            return true
        }
        // 大概缓存15~20条记录便可
        if (sHasCallbackPaths.size >= 20) {
            for (i in 0..4) {
                sHasCallbackPaths.removeAt(0)
            }
        }
        sHasCallbackPaths.add(imagePath)
        return false
    }

    /**
     * 获取屏幕分辨率
     */
    private val realScreenSize: Point
        private get() {
            var screenSize: Point? = null
            try {
                screenSize = Point()
                val windowManager =
                    mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val defaultDisplay = windowManager.defaultDisplay
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    defaultDisplay.getRealSize(screenSize)
                } else {
                    try {
                        val mGetRawW =
                            Display::class.java.getMethod("getRawWidth")
                        val mGetRawH =
                            Display::class.java.getMethod("getRawHeight")
                        screenSize[(mGetRawW.invoke(defaultDisplay) as Int)] =
                            (mGetRawH.invoke(defaultDisplay) as Int)
                    } catch (e: Exception) {
                        screenSize[defaultDisplay.width] = defaultDisplay.height
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return screenSize!!
        }

    //    public Bitmap createScreenShotBitmap(Context context, String screenFilePath) {
//        View v = LayoutInflater.from(context).inflate(R.layout.share_screenshot_layout, null);
//        ImageView iv = (ImageView) v.findViewById(R.id.iv);
//        Bitmap bitmap = BitmapFactory.decodeFile(screenFilePath);
//        iv.setImageBitmap(bitmap);
//整体布局
//        Point point = getRealScreenSize();
//        v.measure(View.MeasureSpec.makeMeasureSpec(point.x, View.MeasureSpec.EXACTLY),
//                View.MeasureSpec.makeMeasureSpec(point.y, View.MeasureSpec.EXACTLY));
//
//        v.layout(0, 0, point.x, point.y);
//        Bitmap result = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.RGB_565);
//        Bitmap result = Bitmap.createBitmap(v.getWidth(), v.getHeight() + dp2px(context, 140), Bitmap.Config.ARGB_8888);
//        Canvas c = new Canvas(result);
//        c.drawColor(Color.WHITE);
// Draw view to canvas
//        v.draw(c);
//        return result;
//    }
    private fun dp2px(ctx: Context, dp: Float): Int {
        val scale = ctx.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    /**
     * 设置截屏监听器
     */
    fun setListener(listener: OnScreenShotListener?) {
        mListener = listener
    }

    interface OnScreenShotListener {
        fun onShot(imagePath: String?)
    }

    /**
     * 媒体内容观察者(观察媒体数据库的改变)
     */
    private inner class MediaContentObserver(
        private val mContentUri: Uri,
        handler: Handler?
    ) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handleMediaContentChange(mContentUri)
        }

    }

    companion object {
        private const val TAG = "ScreenShotListenManager"
        /**
         * 读取媒体数据库时需要读取的列
         */
        private val MEDIA_PROJECTIONS = arrayOf(
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DATE_TAKEN
        )
        /**
         * 读取媒体数据库时需要读取的列, 其中 WIDTH 和 HEIGHT 字段在 API 16 以后才有
         */
        private val MEDIA_PROJECTIONS_API_16 = arrayOf(
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.ImageColumns.WIDTH,
            MediaStore.Images.ImageColumns.HEIGHT
        )
        /**
         * 截屏依据中的路径判断关键字
         */
        private val KEYWORDS = arrayOf(
            "screenshot", "screen_shot", "screen-shot", "screen shot",
            "screencapture", "screen_capture", "screen-capture", "screen capture",
            "screencap", "screen_cap", "screen-cap", "screen cap"
        )
        private var sScreenRealSize: Point? = null
        /**
         * 已回调过的路径
         */
        private val sHasCallbackPaths: MutableList<String> =
            ArrayList()

        fun newInstance(context: Context?): ScreenShotListenManager {
            assertInMainThread()
            return ScreenShotListenManager(context)
        }

        private fun assertInMainThread() {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                val elements =
                    Thread.currentThread().stackTrace
                var methodMsg: String? = null
                if (elements != null && elements.size >= 4) {
                    methodMsg = elements[3].toString()
                }
                throw IllegalStateException("Call the method must be in main thread: $methodMsg")
            }
        }
    }

    init {
        requireNotNull(context) { "The context must not be null." }
        mContext = context
        // 获取屏幕真实的分辨率
        if (sScreenRealSize == null) {
            sScreenRealSize = realScreenSize
            if (sScreenRealSize != null) {
                Log.d(
                    TAG,
                    "Screen Real Size: " + sScreenRealSize?.x + " * " + sScreenRealSize?.y
                )
            } else {
                Log.w(
                    TAG,
                    "Get screen real size failed."
                )
            }
        }
    }
}