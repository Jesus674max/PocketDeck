package com.pmmpdeck

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Ejecuta comandos en Termux con el intent RUN_COMMAND y devuelve el stdout. */
object TermuxBridge {
    private const val ACTION = "com.pmmpdeck.TERMUX_RESULT"
    private const val PREFIX = "/data/data/com.termux/files/usr/bin"
    private const val HOME = "/data/data/com.termux/files/home"
    private var nextId = 1000
    private val callbacks = HashMap<Int, (String, String) -> Unit>()

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val id = i.getIntExtra("id", -1)
            val b = i.getBundleExtra("result")
            val out = b?.getString("stdout") ?: ""
            val err = b?.getString("errmsg") ?: b?.getString("stderr") ?: ""
            callbacks.remove(id)?.invoke(out, err)
        }
    }
    val filter get() = android.content.IntentFilter(ACTION)

    fun run(ctx: Context, script: String, cb: ((String, String) -> Unit)? = null) {
        val i = Intent("com.termux.RUN_COMMAND").apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "$PREFIX/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS",
                arrayOf("-c", "export PATH=$PREFIX:\$PATH; $script"))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            if (cb != null) {
                val id = nextId++
                callbacks[id] = cb
                val r = Intent(ACTION).setPackage(ctx.packageName).putExtra("id", id)
                putExtra("com.termux.RUN_COMMAND_PENDING_INTENT",
                    PendingIntent.getBroadcast(ctx, id, r,
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            }
        }
        try { ctx.startService(i) }
        catch (e: Exception) { cb?.invoke("", "No se pudo hablar con Termux: ${e.message}") }
    }

    // --- Comandos del servidor (sesión tmux "pmmp") ---
    fun start(c: Context) = run(c,
        "termux-wake-lock; tmux has-session -t pmmp 2>/dev/null || tmux new-session -d -s pmmp 'cd ~/pocketmine && ./start.sh'")
    fun stop(c: Context) = run(c, "tmux send-keys -t pmmp stop Enter")
    fun kill(c: Context) = run(c, "tmux kill-session -t pmmp; termux-wake-unlock")
    fun send(c: Context, text: String) {
        val q = text.replace("'", "'\\''")
        run(c, "tmux send-keys -t pmmp -l '$q'; tmux send-keys -t pmmp Enter")
    }
    fun status(c: Context, cb: (String, String) -> Unit) = run(c,
        "if tmux has-session -t pmmp 2>/dev/null; then echo ON; tmux capture-pane -p -t pmmp -S -80; else echo OFF; fi", cb)
}
