package com.fab365app.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.app.uxis.lf.util.CustomAlert
import java.io.File
import java.net.URISyntaxException

class BaseWebView : WebView {

    companion object {
        private const val TAG = "BaseWebView"
        var mContext: Context? = null
        lateinit var mWebView: BaseWebView
        lateinit var activity: MainActivity
    }

    lateinit var subWebViewInstance: WebView

    fun setActivity(activity: MainActivity) {
        Companion.activity = activity
    }

    fun setSubWebView(subWebView: WebView) {
        this.subWebViewInstance = subWebView
    }

    constructor(context: Context) : super(context) {
        mContext = context
        initializeOptions()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mContext = context
        initializeOptions()
    }

    init {
        initializeOptions()
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeOptions() {
        try {
            val cachePath =
                context.cacheDir.absolutePath + "/WebView/Default/HTTP Cache/Code Cache/js"
            val cacheDir = File(cachePath)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // WebView 설정
            if (BuildConfig.DEBUG) {
                setWebContentsDebuggingEnabled(true)
            }

            val webSettings: WebSettings = this.settings
            webSettings.apply {
                loadsImagesAutomatically = true
                javaScriptEnabled = true    // 웹페이지 자바스크립트 허용 여부
                setSupportMultipleWindows(true) //멀티윈도우를 지원할지 여부
                javaScriptCanOpenWindowsAutomatically = true
                loadWithOverviewMode = true //컨텐츠가 웹뷰보다 클때 스크린 크기에 맞추기
                useWideViewPort = true  // 화면 사이즈 맞추기 허용 여부

                domStorageEnabled = true     //DOM 로컬 스토리지 사용여부
                databaseEnabled = true  //database storage API 사용 여부
                allowFileAccess = true  //파일 액세스 허용 여부
                allowContentAccess = true    //Content URL 에 접근 사용 여부

                textZoom = 100  // system 글꼴 크기에 의해 변하는 것 방지

                setSupportZoom(true)    // 화면 줌 허용 여부
                builtInZoomControls = true  // 줌 아이콘
                displayZoomControls = false // 웹뷰 화면에 보이는 (+/-) 줌 아이콘

                //Google 로그인/회원가입을 위한 user-agent 설정
                setUserAgentString(getUserAgentString().replace("; wv", ""));


                // user-agent에 ",hazzys@LF" 등을 추가 하여 Web 에서 App 인지를 판단 하게 한다.
//            setUserAgent(webSettings)

                // https -> http 호출 허용
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING   // 컨텐츠 사이즈 자동 맞추기
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            // 기타 필요한 설정

            // 서드파티 쿠키 허용.
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            // App <----> Javascript 통신객체 생성
//        addJavascriptInterface(AndroidScriptBridge(this), "lfsquare")
//        addJavascriptInterface(AndroidScriptBridge(this), "Android")

            // WebViewClient 설정
            webViewClient = MyWebViewClient()
            // WebChromeClient 설정
            webChromeClient = MyWebChromeClient()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            Log.e(TAG, "shouldOverrideUrlLoading : " + request.url.toString())
            val url = request.url.toString()
            return try {
                when {
                    url.startsWith("about:blank") -> {
                        Log.e(TAG, "about:blank")
                        true
                    }

                    url.startsWith("tel:") -> {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
                        mContext!!.startActivity(intent)
                        true
                    }

                    url.startsWith("mailto:") -> {
                        val eMail = url.replace("mailto", "")
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.setType("plain/text")
                        intent.putExtra(Intent.EXTRA_EMAIL, eMail)
                        mContext!!.startActivity(intent)
                        true
                    }

                    url.startsWith("https://maps.google.com") -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        mContext!!.startActivity(intent)
                        true
                    }

                    url.startsWith("intent:kakao") || url.startsWith("kakao") -> {
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            val existPackage = mContext!!.packageManager.getLaunchIntentForPackage(
                                intent.getPackage()!!
                            )
                            if (existPackage != null) {
                                mContext!!.startActivity(intent)
                            } else {
                                val marketIntent = Intent(Intent.ACTION_VIEW)
                                marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()))
                                mContext!!.startActivity(marketIntent)
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Bad URI " + url + ":" + e.message)
                            return false
                        }
                        true
                    }

                    !url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("javascript:") -> {
                        Log.d(TAG, "Exception1: $url")
                        var intent: Intent? = null
                        intent = try {
                            // 딥링크 스키마 확인
                            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        } catch (ex: URISyntaxException) {
                            Log.e(
                                TAG,
                                "[error] Bad request uri format : [" + url + "] =" + ex.message
                            )
                            return false
                        }
                        if (intent != null && mContext!!.packageManager.resolveActivity(
                                intent,
                                0
                            ) == null
                        ) {
                            Log.d(TAG, "shouldOverrideUrlLoading: $url")
                            val pkgName = intent.getPackage()
                            if (pkgName != null) {
                                val uri = Uri.parse("market://search?q=pname:$pkgName")
                                intent = Intent(Intent.ACTION_VIEW, uri)
                                mContext!!.startActivity(intent)
                            }
                        } else {
                            val uri = Uri.parse(intent?.dataString)
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            mContext?.startActivity(intent)
                        }
                        true
                    }

                    else -> {
                        // 새로운 웹뷰를 생성하여 로드
                        val subWebView = findViewById<WebView>(R.id.sub_web_view)
                        Log.e(TAG, "subWebView : [$url]")
                        subWebView.visibility = View.VISIBLE
                        subWebView.loadUrl(url)
                        true
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Exception: $url")
                false
            }
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String, isReload: Boolean) {
            Log.d(TAG, "doUpdateVisitedHistory : $url")
        }

        // 페이지 로딩 시작
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Log.e(TAG, "onPageStarted URL : $url")

        }

        // 오류 처리
        @Deprecated("Deprecated in Java")
        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String,
            failingUrl: String
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            Log.e(TAG, "onReceivedError : $failingUrl")
            handleError(errorCode)
        }

        // 페이지 로딩 완료
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            Log.e(TAG, "onPageFinished : $url")
            // 웹뷰의 RAM과 영구 저장소 사이에 쿠키 강제 동기화 수행 함.
            CookieManager.getInstance().flush()
        }

        private fun handleError(errorCode: Int) {
            when (errorCode) {
                ERROR_AUTHENTICATION -> Log.e(TAG, "onReceivedError : 서버에서 사용자 인증 실패")
                ERROR_BAD_URL -> Log.e(TAG, "onReceivedError : 잘못된 URL")
                ERROR_CONNECT -> Log.e(TAG, "onReceivedError : 서버로 연결 실패")
                ERROR_FAILED_SSL_HANDSHAKE -> Log.e(TAG, "onReceivedError : SSL handshake 수행 실패")
                ERROR_FILE -> Log.e(TAG, "onReceivedError : 일반 파일 오류")
                ERROR_FILE_NOT_FOUND -> Log.e(TAG, "onReceivedError : 파일을 찾을 수 없습니다")
                ERROR_HOST_LOOKUP -> Log.e(TAG, "onReceivedError : 서버 또는 프록시 호스트 이름 조회 실패")
                ERROR_IO -> Log.e(TAG, "onReceivedError : 서버에서 읽거나 서버로 쓰기 실패")
                ERROR_PROXY_AUTHENTICATION -> Log.e(TAG, "onReceivedError : 프록시에서 사용자 인증 실패")
                ERROR_REDIRECT_LOOP -> Log.e(TAG, "onReceivedError : 너무 많은 리디렉션")
                ERROR_TIMEOUT -> Log.e(TAG, "onReceivedError : 연결 시간 초과")
                ERROR_TOO_MANY_REQUESTS -> Log.e(TAG, "onReceivedError : 페이지 로드중 너무 많은 요청 발생")
                ERROR_UNKNOWN -> Log.e(TAG, "onReceivedError : 일반 오류")
                ERROR_UNSUPPORTED_AUTH_SCHEME -> Log.e(TAG, "onReceivedError : 지원되지 않는 인증 체계")
                ERROR_UNSUPPORTED_SCHEME -> Log.e(TAG, "onReceivedError : URI가 지원되지 않는 방식")
            }
        }
    }


    private inner class MyWebChromeClient : WebChromeClient() {
        // webview에 있는 inline 동영상 player가 영상을  load 할 때, 보이는
        // 회색 play button이 안보이게 한다.
        // (web에서 meta_tag로 poster를 설정 하면 이런 현상이 발생되어 App에서 제거처리를 해야 함)
        override fun getDefaultVideoPoster(): Bitmap {
            return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        }

        @SuppressLint("SetJavaScriptEnabled")
        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message
        ): Boolean {
            activity.binding.subWebViewContainer.removeAllViews()

            // 새 WebView 인스턴스 생성
            val newWebView = WebView(view.context)

            // 새로운 WebView 설정
            newWebView.apply {
                webViewClient = MyWebViewClient()
                webChromeClient = this@MyWebChromeClient
                settings.apply {
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    setSupportMultipleWindows(false) //멀티윈도우를 지원할지 여부
                    useWideViewPort = true  // 화면 사이즈 맞추기 허용 여부

                }
            }

            // 새 WebView를 subWebViewInstance에 추가
            subWebViewInstance = newWebView
            activity.binding.subWebViewContainer.apply {
                visibility = View.VISIBLE
                addView(newWebView)
            }

            // WebViewTransport를 사용하여 새 WebView 전달
            val transport = resultMsg.obj as WebView.WebViewTransport
            transport.webView = subWebViewInstance
            resultMsg.sendToTarget()

            return true
        }

        override fun onCloseWindow(window: WebView) {
            super.onCloseWindow(window)
            // 서브 웹뷰 숨기기
            activity.binding.subWebViewContainer.apply {
                visibility = View.GONE
                removeView(window)
                window.destroy()
            }
        }

        //웹뷰 alert 네이티브 팝업처리
        override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult
        ): Boolean {
            val myAlert = CustomAlert(
                mContext!!, message!!, "확인", { dialog, which -> result.confirm() })
            myAlert.show()
            return true
        }

        override fun onJsConfirm(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult
        ): Boolean {
            val myAlert = CustomAlert(
                mContext!!, message!!, "확인", "취소",
                { dialog, which ->
                    result.confirm() // 확인
                }, { dialog, which ->
                    result.cancel() // 취소
                })
            myAlert.show()
            return true
        }

        //파일 업로드 관련 이벤트 처리
        fun openFileChooser(uploadMsg: ValueCallback<Uri?>?) {
            (mContext as MainActivity).doFileAttach(uploadMsg!!)
            Log.d(TAG, "openFileChooser")
        }

        // 4.1
        @Suppress("unused")
        fun openFileChooser(uploadMsg: ValueCallback<Uri?>?, acceptType: String?) {
            openFileChooser(uploadMsg)
        }

        // 4.4
        @Suppress("unused")
        fun openFileChooser(
            uploadMsg: ValueCallback<Uri?>?,
            acceptType: String?,
            capture: String?
        ) {
            openFileChooser(uploadMsg)
        }

        // 5.0 이후, Build Sdk를 5.0 이상을 적용해야함.
        override fun onShowFileChooser(
            webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            (mContext as MainActivity).doFileAttachs(filePathCallback)
            return true
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            super.onShowCustomView(view, callback)
            Log.d(TAG, "onShowCustomView()")
        }
    }
}