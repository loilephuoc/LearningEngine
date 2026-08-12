package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.desktop.ui.browser.ContentDraftEdits

fun Content.toStudyQuickEditDraft() = ContentDraftEdits(
    contentId = id.value,
    questionText = text.primaryText,
    answerText = text.translatedText.orEmpty(),
    exampleText = text.exampleText.orEmpty(),
    exampleTranslation = text.exampleTranslation.orEmpty(),
    pronunciation = text.pronunciation.orEmpty(),
    partOfSpeech = customFields[ContentFieldId("partOfSpeech")]?.value ?: "WORD",
    imageRef = media.image,
    questionAudioRef = media.primaryAudio,
    answerAudioRef = media.translatedAudio,
    exampleAudioRef = media.exampleAudio,
    translationAudioRef = media.exampleTranslatedAudio
)

class StudyQuickEditProjectionException(cause: Exception) :
    IllegalStateException("Content was saved, but the current Study item could not be refreshed.", cause)
