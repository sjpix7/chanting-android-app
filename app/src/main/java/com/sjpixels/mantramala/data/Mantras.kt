package com.sjpixels.mantramala.data

import androidx.annotation.StringRes
import com.sjpixels.mantramala.R

/** A built-in mantra. Sanskrit text and transliteration are universal; title and meaning are localised. */
data class Mantra(
    val key: String,
    val devanagari: String,
    val transliteration: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val meaningRes: Int,
)

object Mantras {
    /** The mantra a new user starts with; they can change it in the library or add their own. */
    const val DEFAULT_KEY = "om_namah_shivaya"
    const val CUSTOM_PREFIX = "custom:"

    val builtIn: List<Mantra> = listOf(
        Mantra(
            "om_namah_shivaya", "ॐ नमः शिवाय", "Oṃ Namaḥ Śivāya",
            R.string.m_shiva_title, R.string.m_shiva_meaning,
        ),
        Mantra(
            "hare_krishna",
            "हरे कृष्ण हरे कृष्ण कृष्ण कृष्ण हरे हरे\nहरे राम हरे राम राम राम हरे हरे",
            "Hare Kṛṣṇa Hare Kṛṣṇa Kṛṣṇa Kṛṣṇa Hare Hare · Hare Rāma Hare Rāma Rāma Rāma Hare Hare",
            R.string.m_hare_krishna_title, R.string.m_hare_krishna_meaning,
        ),
        Mantra(
            "gayatri",
            "ॐ भूर्भुवः स्वः\nतत्सवितुर्वरेण्यं भर्गो देवस्य धीमहि\nधियो यो नः प्रचोदयात्",
            "Oṃ Bhūr Bhuvaḥ Svaḥ · Tat Savitur Vareṇyaṃ Bhargo Devasya Dhīmahi · Dhiyo Yo Naḥ Pracodayāt",
            R.string.m_gayatri_title, R.string.m_gayatri_meaning,
        ),
        Mantra(
            "mahamrityunjaya",
            "ॐ त्र्यम्बकं यजामहे सुगन्धिं पुष्टिवर्धनम्\nउर्वारुकमिव बन्धनान् मृत्योर्मुक्षीय मामृतात्",
            "Oṃ Tryambakaṃ Yajāmahe Sugandhiṃ Puṣṭivardhanam · Urvārukamiva Bandhanān Mṛtyor Mukṣīya Māmṛtāt",
            R.string.m_mahamrityunjaya_title, R.string.m_mahamrityunjaya_meaning,
        ),
        Mantra(
            "vasudevaya", "ॐ नमो भगवते वासुदेवाय", "Oṃ Namo Bhagavate Vāsudevāya",
            R.string.m_vasudeva_title, R.string.m_vasudeva_meaning,
        ),
        Mantra(
            "shri_ram", "श्री राम जय राम जय जय राम", "Śrī Rāma Jaya Rāma Jaya Jaya Rāma",
            R.string.m_ram_title, R.string.m_ram_meaning,
        ),
        Mantra(
            "radhe", "राधे राधे", "Rādhe Rādhe",
            R.string.m_radhe_title, R.string.m_radhe_meaning,
        ),
        Mantra(
            "ganesha", "ॐ गं गणपतये नमः", "Oṃ Gaṃ Gaṇapataye Namaḥ",
            R.string.m_ganesha_title, R.string.m_ganesha_meaning,
        ),
        Mantra(
            "hanuman", "ॐ हं हनुमते नमः", "Oṃ Haṃ Hanumate Namaḥ",
            R.string.m_hanuman_title, R.string.m_hanuman_meaning,
        ),
        Mantra(
            "narayana", "ॐ नमो नारायणाय", "Oṃ Namo Nārāyaṇāya",
            R.string.m_narayana_title, R.string.m_narayana_meaning,
        ),
        Mantra(
            "saraswati", "ॐ ऐं सरस्वत्यै नमः", "Oṃ Aiṃ Sarasvatyai Namaḥ",
            R.string.m_saraswati_title, R.string.m_saraswati_meaning,
        ),
        Mantra(
            "lakshmi", "ॐ श्रीं महालक्ष्म्यै नमः", "Oṃ Śrīṃ Mahālakṣmyai Namaḥ",
            R.string.m_lakshmi_title, R.string.m_lakshmi_meaning,
        ),
        Mantra(
            "durga", "ॐ दुं दुर्गायै नमः", "Oṃ Duṃ Durgāyai Namaḥ",
            R.string.m_durga_title, R.string.m_durga_meaning,
        ),
        Mantra(
            "om", "ॐ", "Oṃ",
            R.string.m_om_title, R.string.m_om_meaning,
        ),
    )

    private val byKey = builtIn.associateBy { it.key }

    fun builtIn(key: String): Mantra? = byKey[key]

    fun customKey(id: Long) = "$CUSTOM_PREFIX$id"

    fun customId(key: String): Long? =
        if (key.startsWith(CUSTOM_PREFIX)) key.removePrefix(CUSTOM_PREFIX).toLongOrNull() else null
}
