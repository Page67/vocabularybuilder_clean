package com.shiki.vocabulary

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shiki.vocabulary.data.CurrentUnitStore
import com.shiki.vocabulary.data.ProgressStore
import com.shiki.vocabulary.data.UnitContentRepository
import com.shiki.vocabulary.ui.ContentLoadError
import com.shiki.vocabulary.ui.VocabularyApp
import com.shiki.vocabulary.ui.theme.VocabularyTheme
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(this, this)
        val content = runCatching { UnitContentRepository.load(this) }
        setContent {
            VocabularyTheme {
                content.fold(
                    onSuccess = { library ->
                        val currentUnitStore = CurrentUnitStore(this)
                        val availableUnits = library.catalog.units.map { it.unitId }.toSet()
                        VocabularyApp(
                            library = library,
                            initialUnitId = currentUnitStore.load(availableUnits)
                                ?: library.catalog.units.first().unitId,
                            progressStoreFactory = { unitId -> ProgressStore(this, unitId) },
                            onUnitChanged = currentUnitStore::save,
                            onSpeak = ::speak,
                        )
                    },
                    onFailure = { ContentLoadError(it.message ?: "未知内容错误") },
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
        }
    }

    private fun speak(word: String) {
        textToSpeech?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word-$word")
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }
}
