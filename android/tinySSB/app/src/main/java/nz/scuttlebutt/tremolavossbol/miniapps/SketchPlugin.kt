package nz.scuttlebutt.tremolavossbol.miniapps

import android.util.Log
import android.webkit.WebView
import nz.scuttlebutt.tremolavossbol.MainActivity
import nz.scuttlebutt.tremolavossbol.MiniAppPlugin

/**
 * SketchPlugin is a subclass of MiniAppPlugin that provides functionality specific to the Sketch board mini-app.
 * This class handles the initialization of Sketch-related UI elements, manages requests from the web view,
 * and interacts with the Sketch interface by publishing updates and handling various operations.
 */
class SketchPlugin(act: MainActivity, webView: WebView) : MiniAppPlugin(act, webView) {

    /**
     * Initializes the Sketch plugin by injecting JavaScript, and HTML content into the WebView.
     * Also sets up the UI elements and configurations specific to the Sketch mini App.
     */
    override fun initialize() {

        val pakoScript = "miniApps/sketch/src/pako.min.js"
        val sketchUIScript = "miniApps/sketch/src/sketch_ui.js"
        val sketchScript = "miniApps/sketch/src/sketch.js"

        val scriptPath = arrayOf(pakoScript, sketchUIScript, sketchScript)

        // Define scenarioDisplay map for Kanban
        val scenarioDisplay = mapOf(
            "sketch" to listOf("div:back", "core", "lst:chats", "plus")
        )

        val manifestPath = "web/miniApps/sketch/manifest.json"

        injectAll(null, scriptPath, null, null, scenarioDisplay, null, manifestPath)
    }

    /**
     * Handles requests from the web view. Manages different types of actions based on the request type.
     */
    override fun handleRequest(args: List<String>) {
        Log.d("Inside the Sketch handleRequest", args[0])
        when (args[0]) {
            "backPressed" -> {
                val jsCode = """
                    if (curr_scenario == 'sketch') {
                        setScenario('miniapps'); 
                    } else if (curr_scenario == 'posts' && prev_scenario == 'sketch') {
                        setScenario('miniapps'); 
                        
                    }
                """.trimIndent()

                webView.post(Runnable {
                    webView.evaluateJavascript(jsCode, null)
                })
            }
            "load_chat_extension" -> {
                val jsCode = """
                    if (prev_scenario == 'sketch') {
                        prev_scenario = 'miniapps'
                        chat_openSketch();
                    }
                """.trimIndent()

                webView.post(Runnable {
                    webView.evaluateJavascript(jsCode, null)
                })
            }
            "sketch:plus_button" -> {
                val jsCode = """
                    launch_snackbar("This feature is currently deactivated")
                """.trimIndent()

                webView.post(Runnable {
                    webView.evaluateJavascript(jsCode, null)
                })
            }
        }
    }

}