import kotlinx.cinterop.*
import gtk3.*

fun activate(app: CPointer<GtkApplication>, user_data: gpointer ) {
    val windowWidget = gtk_application_window_new(app)!!
    val window = windowWidget.reinterpret<GtkWindow>()
    gtk_window_set_title(window, "Window");
    gtk_window_set_default_size(window, 200, 200)
    gtk_widget_show_all(windowWidget)
}

/*
inline fun <T1, T2> g_connect(obj: T1, actionName: String, action: CFunctionPointer<T2>) {
    g_signal_connect_data(obj, actionName, action.reinterpret<GCallback>().pointed,
            data = null, destroy_data = null, connect_flags = 0)

}*/

fun gtkMain(args: Array<String>): Int {
    val app = gtk_application_new("org.gtk.example", G_APPLICATION_FLAGS_NONE)!!
    g_signal_connect_data(app, "activate",
            staticCFunction(::activate).reinterpret<GCallback>(),
            data = null, destroy_data = null, connect_flags = 0)
    val status = memScoped {
        g_application_run(app.reinterpret<GApplication>(),
                args.size, args.map { it.cstr.getPointer(memScope) }.toCValues())
    }
    g_object_unref(app)
    return status
}

fun main(args: Array<String>) {
    gtkMain(args)
}