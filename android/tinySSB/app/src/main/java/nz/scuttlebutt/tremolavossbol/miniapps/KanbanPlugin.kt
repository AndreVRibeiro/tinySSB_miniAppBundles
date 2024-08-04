package nz.scuttlebutt.tremolavossbol.miniapps

import android.util.Base64
import android.util.Log
import android.webkit.WebView
import nz.scuttlebutt.tremolavossbol.MainActivity
import nz.scuttlebutt.tremolavossbol.miniapps.MiniAppPlugin
import nz.scuttlebutt.tremolavossbol.utils.Bipf

// Define a constant for the Kanban app within the plugin
val TINYSSB_APP_KANBAN       = Bipf.mkString("KAN") // ...

/**
 * KanbanPlugin is a subclass of MiniAppPlugin that provides functionality specific to the Kanban board mini-app.
 * This class handles the initialization of Kanban-related UI elements, manages requests from the web view,
 * and interacts with the Kanban board by publishing updates and handling various operations.
 */
class KanbanPlugin(act: MainActivity, webView: WebView) : MiniAppPlugin(act, webView) {

    /**
     * Initializes the Kanban plugin by injecting CSS, JavaScript, and HTML content into the WebView.
     * Also sets up the UI elements and configurations specific to the Kanban board.
     */
    override fun initialize() {
        // Define the CSS styles for the Kanban board UI
        val css = "miniApps/board/resources/kanban.css"

        val boardScript = "miniApps/board/src/board.js"
        val boardUIScript = "miniApps/board/src/board_ui.js"

        val scriptPaths = arrayOf(boardScript, boardUIScript)

        val htmlContents = "web/miniApps/board/resources/kanban.html"

        // Define displayOrNot list for Kanban
        val displayOrNot = listOf(
            "lst:kanban",
            "div:board"
        )

        // Define scenarioDisplay map for Kanban
        val scenarioDisplay = mapOf(
            "kanban" to listOf("div:back", "core", "lst:kanban", "plus"),
            "board" to listOf("div:back", "core", "div:board")
        )

        // Define scenarioMenu map for Kanban
        val scenarioMenu = mapOf(
            "kanban" to listOf(
                "New Kanban board" to "menu_new_board",
                "Invitations" to "menu_board_invitations",
                "Connected Devices" to "menu_connection",
                "Settings" to "menu_settings",
                "About" to "menu_about"
            ),
            "board" to listOf(
                "Add list" to "menu_new_column",
                "Rename Kanban Board" to "menu_rename_board",
                "Invite Users" to "menu_invite",
                "History" to "menu_history",
                "Reload" to "reload_curr_board",
                "Leave" to "leave_curr_board",
                "(un)Forget" to "board_toggle_forget",
                "Debug" to "ui_debug"
            )
        )

        val manifestPath = "web/miniApps/board/manifest.json"

        injectAll(css, scriptPaths, htmlContents, displayOrNot, scenarioDisplay, scenarioMenu, manifestPath)
    }

    /**
     * Handles requests from the web view. Manages different types of actions based on the request type.
     */
    override fun handleRequest(args: List<String>) {
        Log.d("Inside the Kanban handleRequest", args[0])
        when(args[0]) {
            "kanban" -> {
                // Kanban-specific logic
                val bid: String? = if (args[1] != "null") args[1] else null
                val prev: List<String>? = if (args[2] != "null") Base64.decode(args[2], Base64.NO_WRAP).decodeToString().split(",").map{ Base64.decode(it, Base64.NO_WRAP).decodeToString()} else null
                val op: String = args[3]
                val argsList: List<String>? = if(args[4] != "null") Base64.decode(args[4], Base64.NO_WRAP).decodeToString().split(",").map{ Base64.decode(it, Base64.NO_WRAP).decodeToString()} else null

                kanban(bid, prev , op, argsList)
            }
            "backPressed" -> {
                val jsCode = """
                    if (curr_scenario == 'board') {
                        setKanbanScenario('kanban');
                    } else if (curr_scenario == 'kanban'){
                        setScenario('miniapps'); 
                    }
                """.trimIndent()

                eval(jsCode)
            }
            "kanban:members_confirmed" -> {
                eval("menu_new_board_name()")
            }
            "kanban:plus_button" -> {
                eval("menu_new_board()")
            }
            "edit_confirmed" -> {
                eval("kanban_edit_confirmed()")
            }
            "b2f_initialize" -> {
                eval("load_board_list()")
            }
            "b2f_new_event" -> {
                eval("load_board_list()")
            }
        }
    }

    /**
     * Executes Kanban-specific operations based on the provided arguments. This includes
     * publishing content to the Kanban board with operations and arguments.
     *
     * @param bid The board ID.
     * @param prev The list of previous board states.
     * @param operation The operation to perform (e.g., create, update).
     * @param args Additional arguments for the operation.
     */
    fun kanban(bid: String?, prev: List<String>?, operation: String, args: List<String>?) {
        val lst = Bipf.mkList()
        Bipf.list_append(lst, TINYSSB_APP_KANBAN)
        if (bid != null)
            Bipf.list_append(lst, Bipf.mkBytes(Base64.decode(bid, Base64.NO_WRAP)))
        else
            Bipf.list_append(lst, Bipf.mkNone())

        if(prev != null) {
            val prevList = Bipf.mkList()
            for(p in prev) {
                Bipf.list_append(prevList, Bipf.mkBytes(Base64.decode(p, Base64.NO_WRAP)))
            }
            Bipf.list_append(lst, prevList)
        } else {
            Bipf.list_append(lst, Bipf.mkString("null"))  // TODO: Change to Bipf.mkNone(), but would be incompatible with the old format
        }

        Bipf.list_append(lst, Bipf.mkString(operation))

        if(args != null) {
            for(arg in args) {
                if (Regex("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?\$").matches(arg)) {
                    Bipf.list_append(lst, Bipf.mkBytes(Base64.decode(arg, Base64.NO_WRAP)))
                } else { // arg is not a b64 string
                    Bipf.list_append(lst, Bipf.mkString(arg))
                }
            }
        }

        val body = Bipf.encode(lst)

        if (body != null) {
            Log.d("kanban", "published bytes: " + Bipf.decode(body))
            act.tinyNode.publish_public_content(body)
        }
        //val body = Bipf.encode(lst)
        //Log.d("KANBAN BIPF ENCODE", Bipf.bipf_list2JSON(Bipf.decode(body!!)!!).toString())
        //if (body != null)
        //act.tinyNode.publish_public_content(body)

    }

}