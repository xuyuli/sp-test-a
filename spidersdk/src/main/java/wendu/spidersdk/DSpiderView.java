package wendu.spidersdk;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.LinearLayout;
import org.json.JSONObject;
import java.util.Map;

/**
 * Created by du on 16/4/19.
 */
public class DSpiderView extends LinearLayout {

    private DSWebview webview;
    private ViewGroup loading;
    private SpiderEventListener spiderEventListener;
    private int max = 100;
    private int sid=0;
    private int retry=1;
    private int mScriptCount=1;
    private String startUrl="";
    private boolean customProgressShow=false;
    private boolean errorCanRetry=true;
    private String arguments;

    private final int UA_MOBILE=1;
    private final int UA_PC=2;
    private final int UA_AUTO=3;
    private String defaultUA;

    public DSpiderView(Context context) {
        super(context);
        init();
    }

    public DSpiderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        LayoutInflater.from(getContext()).inflate(R.layout.dspider_view, this);
        webview= (DSWebview) findViewById(R.id.ds_webview);
        defaultUA=webview.getSettings().getUserAgentString();
        loading= (ViewGroup) findViewById(R.id.ds_loading);
        webview.setWebEventListener(new DSWebview.WebEventListener() {
            @Override
            void onPageStart(String url) {
                if(!(TextUtils.isEmpty(webview.getExceptUrl())||webview.getExceptUrl().equals(url))){
                    customProgressShow=true;
                    spiderEventListener.onProgressShow(true);
                }else{
                    loading.setVisibility(VISIBLE);
                }
                webview.enableFocus(!customProgressShow);
            }

            @Override
            void onPageFinished(String url) {
                super.onPageFinished(url);
                loading.setVisibility(GONE);
                webview.enableFocus(!customProgressShow);
            }

            @Override
            void onReceivedError(String msg) {
                super.onReceivedError(msg);
                if (spiderEventListener != null) {
                    errorCanRetry=false;
                    spiderEventListener.onError(DSpider.Result.STATE_WEB_ERROR, msg);
                }
            }

            @Override
            void onSdkServerError(Exception e) {
                super.onSdkServerError(e);
                if (spiderEventListener != null) {
                    errorCanRetry=false;
                    spiderEventListener.onError(DSpider.Result.STATE_DSPIDER_SERVER_ERROR, e.getMessage());
                }
            }

        });
        addJavaScriptApi();
    }




    private void addJavaScriptApi() {
        webview.removeJavascriptInterface();
        webview.addJavascriptInterface(new JavaScriptBridge(webview, new JavaScriptHandler() {
            @Override
            public void setProgress(int progress) {
                if (spiderEventListener != null) {
                    spiderEventListener.onProgress(progress, max);
                }
            }

            @Override
            public void setProgressMax(int progress) {
                max = progress;
            }

            @Override
            public void finish(DSpider.Result result) {
                if (spiderEventListener != null) {
                    if (result.code == DSpider.Result.STATE_SUCCEED) {
                        spiderEventListener.onResult(result.sessionKey, result.datas);
                    } else {
                        if(result.code==DSpider.Result.STATE_ERROR_MSG
                                ||result.code==DSpider.Result.STATE_DSPIDER_SERVER_ERROR
                               ){
                            errorCanRetry=false;
                        }else {
                            errorCanRetry=true;
                        }
                        spiderEventListener.onError(result.code, result.errorMsg);
                    }
                }
            }

            @Override
            public String getArguments() {
                return arguments;
            }

            @Override
            public void setArguments(String json) {
                arguments=json;
            }

            @Override
            public void setProgressMsg(String msg) {
                if (spiderEventListener != null) {
                    spiderEventListener.onProgressMsg(msg);
                }
            }

            @Override
            public void log(String log, int type) {
                super.log(log, type);
                if (spiderEventListener != null) {
                    spiderEventListener.onLog(log,type);
                }
            }

            @Override
            public void showProgress(boolean show) {
                if (spiderEventListener != null) {
                    spiderEventListener.onProgressShow(show);
                }
                customProgressShow=show;
                //无论show还是false,loading都应该隐藏
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loading.setVisibility(GONE);
                    }
                },100);

            }
        }));
    }

    public boolean canRetry(){
        return retry<mScriptCount && errorCanRetry;
    }

    public void retry(){
        if(canRetry()) {
            start();
        }
    }

    public void startDebug(String startUrl, String debugSrc, @NonNull  SpiderEventListener spiderEventListener) {
        this.spiderEventListener=spiderEventListener;
        addJavaScriptApi();
        CookieManager.getInstance().removeAllCookie();
        webview.setExceptUrl("");
        customProgressShow=false;
        webview.setDebug(true);
        webview.setDebugSrc(debugSrc);
        this.startUrl=startUrl;
        webview.loadUrl(startUrl);
    }

    public  void setArguments(Map<String, Object> arguments){
        try {
            this.arguments=new JSONObject(arguments).toString();
        } catch (Exception e) {
            this.arguments="{}";
        }
    }

    public void clearCache(){
        webview.clearCache(true);
    }

    public void stop(){
        webview.post(new Runnable() {
            @Override
            public void run() {
                webview.removeJavascriptInterface();
                webview.loadUrl("javascript: window.close()");
                CookieManager.getInstance().removeAllCookie();
            }
        });
    }

    public  void setArguments(String  argumentsJson){
        this.arguments=argumentsJson;
    }

    public void start( int sid, @NonNull  SpiderEventListener spiderEventListener) {
        this.sid=sid;
        this.retry=0;
        this.spiderEventListener = spiderEventListener;
        start();
    }

    public DSWebview getWebview(){
        return  webview;
    }

    private void start(){
        final Context ctx = getContext();
        Helper.init((Activity) ctx, sid,++retry, new InitStateListener() {
            @Override
            public void onSucceed(int scriptId, String url, String script,int scriptCount,int taskId, int ua) {
                mScriptCount=scriptCount;
                CookieManager.getInstance().removeAllCookie();
                webview.setExceptUrl("");
                customProgressShow=false;
                addJavaScriptApi();
                webview.setDebug(false);
                webview.setScriptId(scriptId+"");
                webview.setTaskId(taskId + "");
                webview.setInjectScript(script);
                startUrl=url;
                if(ua==UA_MOBILE){
                    webview.setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1");
                }else if(ua==UA_PC){
                    webview.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");
                }else {
                    webview.setUserAgent(defaultUA);
                }
                if(spiderEventListener!=null){
                    spiderEventListener.onProgressShow(false);
                    spiderEventListener.onProgress(0,100);
                    //spiderEventListener.onProgressMsg("任务初始化中...");
                    spiderEventListener.onScriptLoaded(retry);
                }
                webview.loadUrl(url);
            }

            @Override
            public void onFail(final String msg, final int code) {
                spiderEventListener.onError(code, msg);
            }
        });
    }

}
