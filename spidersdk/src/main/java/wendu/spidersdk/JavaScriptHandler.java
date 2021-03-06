package wendu.spidersdk;

/**
 * Created by du on 16/12/23.
 */

public abstract class JavaScriptHandler {
    public abstract void setProgress(int progress);

    public abstract void setProgressMax(int progress);

    public abstract void finish(DSpider.Result result);

    public abstract String getArguments();

    public abstract void setArguments(String json);

    public void log(String log, int type) {

    }

    public void showProgress(boolean show) {

    }

    public void setProgressMsg(String msg) {

    }
}
