package nz.scuttlebutt.tremolavossbol.miniapps

import android.webkit.WebView
import nz.scuttlebutt.tremolavossbol.MainActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Abstract class representing a Mini App Plugin.
 *
 * This class provides methods to inject CSS, JavaScript, HTML content, and various
 * display configurations into a WebView. Each Mini App should extend this class and
 * provide specific implementations for the `initialize` and `handleRequest` methods.
 */
abstract class MiniAppPlugin(val act: MainActivity, val webView: WebView) {

    /**
    * Initializes the Mini App Plugin.
    *
    * This function is called by the PluginLoader when loading all the Plugins and has to be
    * overridden for every MiniApp specific Plugin. It is used to inject all the needed content.
    */
    abstract fun initialize()

    /**
     * Handles requests with the given arguments.
     *
     * @param args List of arguments to handle the request.
     */
    abstract fun handleRequest(args: List<String>)

    /**
     * Injects all specified resources into the WebView. To be used in the overridden initialize()
     * function for every mini App Plugin
     *
     * @param cssPath CSS code to be injected.
     * @param scriptPath Array of JavaScript file paths to be injected.
     * @param htmlPath Array of HTML content to be injected into the core div.
     * @param displayOrNot List of display configurations to be injected.
     * @param scenarioDisplay Map of scenario displays to be injected.
     * @param scenarioMenu Map of scenario menus to be injected.
     */
    open fun injectAll(cssPath: String? = null,
                       scriptPath: Array<String>? = null,
                       htmlPath: String? = null,
                       displayOrNot: List<String>? = null,
                       scenarioDisplay: Map<String, List<String>>? = null,
                       scenarioMenu: Map<String, List<Pair<String, String>>>? = null,
                       manifestPath: String? = null) {
        cssPath?.let { injectCSS(it) }
        scriptPath?.let { injectScript(*it) }
        htmlPath?.let { injectContent(it) }
        displayOrNot?.let { injectDisplayOrNot(it) }
        scenarioDisplay?.let { injectScenarioDisplay(it) }
        scenarioMenu?.let { injectScenarioMenu(it) }
        manifestPath?.let { addMiniAppToList(it) }
    }

    /**
    * Sends a JavaScript string to the WebView frontend for execution.
    *
    * This function posts a Runnable to the WebView, which then evaluates the given
    * JavaScript string within the context of the web page loaded in the WebView.
    *
    * @param js The JavaScript code to be executed in the WebView.
    */
    open fun eval(js: String) { // send JS string to webkit frontend for execution
        webView.post(Runnable {
            webView.evaluateJavascript(js, null)
        })
    }

    /**
     * Injects the given CSS code into the header of the HTML file.
     *
     * @param css CSS code to be injected.
     */
    private fun injectCSS(css: String) {
        val script = """
            (function() {
                var link = document.createElement('link');
                link.rel = 'stylesheet';
                link.type = 'text/css';
                link.href = '$css';
                document.head.appendChild(link);
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    /**
     * Injects the given JavaScript scripts into the header of the HTML file.
     *
     * @param scriptPaths Variable number of JavaScript file paths to be injected.
     */
    private fun injectScript(vararg scriptPaths: String) {
        scriptPaths.forEach { scriptPath ->
            val script = """
                var script = document.createElement('script');
                script.type = 'text/javascript';
                script.charset = 'UTF-8';
                script.src = '$scriptPath';
                document.head.appendChild(script);
            """.trimIndent()
            webView.evaluateJavascript(script, null)
        }
    }

    /**
     * Injects the given HTML content into the div with id 'core'.
     *
     * @param HtmlPath Variable number of HTML content strings to be injected.
     */
    private fun injectContent(HtmlPath: String) {

        val inputStream = act.assets.open(HtmlPath)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val htmlContent = bufferedReader.use { it.readText() }

        // Javascript function that will be executed by evaluateJavascript
        val content = """
            (function() {
                var coreDiv = document.getElementById('core');
                if (coreDiv) {
                    coreDiv.innerHTML += `$htmlContent`;
                    console.log('HTML content added to core div');
                } else {
                    console.log('Core div not found');
                }
            })();
        """
        webView.evaluateJavascript(content, null)
    }

    /**
     * Injects display configurations into the WebView.
     *
     * @param displayOrNot List of display configurations to be injected.
     */
    private fun injectDisplayOrNot(displayOrNot: List<String>) {

        val jsCodeBuilder = StringBuilder()

        displayOrNot.forEach {
            jsCodeBuilder.append("display_or_not.push('$it');")
        }

        // Evaluate the generated JavaScript code in the WebView
        webView.evaluateJavascript(jsCodeBuilder.toString(), null)
    }

    /**
     * Injects scenario display configurations into the WebView.
     *
     * @param scenarioDisplay Map of scenario displays to be injected.
     */
    private fun injectScenarioDisplay(scenarioDisplay: Map<String, List<String>>) {

        val jsCodeBuilder = StringBuilder()

        scenarioDisplay.forEach { (key, values) ->
            val valuesJs = values.joinToString(prefix = "[", postfix = "]") { "'$it'" }
            jsCodeBuilder.append("scenarioDisplay['$key'] = $valuesJs;")
        }

        // Evaluate the generated JavaScript code in the WebView
        webView.evaluateJavascript(jsCodeBuilder.toString(), null)

    }

    /**
     * Injects scenario menu configurations into the WebView.
     *
     * @param scenarioMenu Map of scenario menus to be injected.
     */
    private fun injectScenarioMenu(scenarioMenu: Map<String, List<Pair<String, String>>>) {

        val jsCodeBuilder = StringBuilder()

        scenarioMenu.forEach { (key, values) ->
            val valuesJs = values.joinToString(prefix = "[", postfix = "]") { (name, func) ->
                "['$name', '$func']"
            }
            jsCodeBuilder.append("scenarioMenu['$key'] = $valuesJs;")
        }

        // Evaluate the generated JavaScript code in the WebView
        webView.evaluateJavascript(jsCodeBuilder.toString(), null)

    }

    /**
     * Adds a Mini App to the list by loading its manifest data.
     *
     * @param manifestPath Path to the manifest file.
     */
    private fun addMiniAppToList(manifestPath: String) {
        val assetManager = act.assets
        val inputStream = assetManager.open(manifestPath)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val content = reader.readText()
        reader.close()

        val quotedContent = JSONObject.quote(content)

        webView.evaluateJavascript("handleManifestContent($quotedContent)", null)
    }

}