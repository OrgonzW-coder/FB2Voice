package com.orgonzw.fb2voice

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Typeface
import android.view.ViewGroup
import android.view.Gravity
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var bookText: TextView
    private var speechReady = false
    private var content = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "FB2Voice"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        layout.addView(title)

        status = TextView(this).apply {
            text = "Открой книгу в формате FB2"
            textSize = 16f
            setPadding(0, 20, 0, 20)
        }
        layout.addView(status)

        val openButton = Button(this).apply {
            text = "Открыть книгу FB2"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(intent, 100)
            }
        }
        layout.addView(openButton)

        val speakButton = Button(this).apply {
            text = "Читать вслух"
            setOnClickListener { speakBook() }
        }
        layout.addView(speakButton)

        val stopButton = Button(this).apply {
            text = "Остановить"
            setOnClickListener {
                if (::tts.isInitialized) tts.stop()
            }
        }
        layout.addView(stopButton)

        bookText = TextView(this).apply {
            textSize = 18f
            setPadding(0, 16, 0, 16)
            text = "Текст книги появится здесь."
        }

        val scroll = ScrollView(this)
        scroll.addView(
            bookText,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        layout.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(layout)
    }

    override fun onInit(result: Int) {
        speechReady = result == TextToSpeech.SUCCESS
        if (speechReady) {
            val languageResult = tts.setLanguage(Locale("ru", "RU"))
            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                runOnUiThread {
                    status.text = "На устройстве нет русского голоса TTS."
                }
            } else {
                runOnUiThread {
                    status.text = "Готово. Открой книгу FB2."
                }
            }
        } else {
            runOnUiThread {
                status.text = "Не удалось запустить озвучку."
            }
        }
    }

    @Deprecated("Legacy document picker callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                content = readFb2(uri)
                bookText.text = content
                status.text = "Книга загружена. Символов: ${content.length}"
            } catch (e: Exception) {
                status.text = "Ошибка чтения FB2: ${e.message}"
            }
        }
    }

    private fun readFb2(uri: Uri): String {
        val parser = Xml.newPullParser()
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Не удалось открыть файл" }
            parser.setInput(input, null)

            val result = StringBuilder()
            var insideBody = false
            var event = parser.eventType

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "body") insideBody = true
                        if (insideBody && parser.name in
                            setOf("p", "title", "subtitle", "empty-line")
                        ) result.append("\n\n")
                    }

                    XmlPullParser.TEXT -> {
                        if (insideBody) result.append(parser.text)
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name == "body") insideBody = false
                    }
                }
                event = parser.next()
            }

            return result.toString().trim()
        }
    }

    private fun speakBook() {
        if (!speechReady) {
            status.text = "Озвучка ещё не готова."
            return
        }
        if (content.isBlank()) {
            status.text = "Сначала открой книгу FB2."
            return
        }

        tts.stop()
        val chunks = content.chunked(3000)
        chunks.forEachIndexed { index, chunk ->
            tts.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH
                else TextToSpeech.QUEUE_ADD,
                null,
                "fb2_chunk_$index"
            )
        }
        status.text = "Читаю книгу вслух…"
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
    ) {
        super.onActivityResult(requestCode, resultCode, data)
