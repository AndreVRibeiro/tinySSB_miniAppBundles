package nz.scuttlebutt.tremolavossbol

import android.webkit.WebView
import java.io.InputStreamReader

/**
 * PluginLoader class is responsible for loading and initializing MiniApp plugins.
 * It searches for manifest files of each MiniApp in the specified directory,
 * extracts the class name of the MiniApp plugin, and returns instances of these plugins.
 */
class PluginLoader(val act: MainActivity, val webView: WebView) {

    /**
     * Searches in the miniApps folder for all the manifest files of every MiniApp,
     * extracts the class name of each MiniApp plugin, and returns a list of plugin instances.
     *
     * @return List of MiniAppPlugin instances.
     */
    fun loadPlugins(): List<MiniAppPlugin> {
        val plugins = mutableListOf<MiniAppPlugin>()

        val assetManager = act.assets
        val miniAppsDir = "web/miniApps"
        val miniApps = assetManager.list(miniAppsDir)

        miniApps?.forEach { miniApp ->
            val manifestPath = "$miniAppsDir/$miniApp/manifest.json"
            val manifest = assetManager.open(manifestPath).use {
                InputStreamReader(it).readText()
            }
            val pluginClass = extractPluginClass(manifest)
            if (pluginClass != null) {
                plugins.add(loadPlugin(pluginClass))
            }
        }
        return plugins
    }

    /**
     * Extracts the class name of the MiniApp plugin from the manifest content.
     *
     * @param manifest The manifest content as a JSON string.
     * @return The class name of the MiniApp plugin, or null if not found.
     */
    private fun extractPluginClass(manifest: String): String? {
        // Extract the plugin class name from the manifest JSON
        val regex = """"pluginClass"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(manifest)?.groupValues?.get(1)
    }

    /**
     * Loads and creates an instance of the MiniApp plugin class.
     *
     * @param pluginClass The class name of the MiniApp plugin.
     * @return An instance of the MiniAppPlugin.
     */
    private fun loadPlugin(pluginClass: String): MiniAppPlugin {
        val clazz = Class.forName(pluginClass)
        val constructor = clazz.getConstructor(MainActivity::class.java, WebView::class.java)
        return constructor.newInstance(act, webView) as MiniAppPlugin
    }
}