package com.example.game.data

import android.content.Context
import com.example.game.model.Case
import com.example.game.model.Character
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.random.Random

object CaseRepository {
    private var cachedCases: List<Case>? = null

    fun loadCases(context: Context): List<Case> {
        if (cachedCases == null) {
            val jsonString = context.resources.openRawResource(R.raw.cases).bufferedReader().use { it.readText() }
            val jsonCases = Json.decodeFromString<List<JsonCase>>(jsonString)
            cachedCases = jsonCases.map { it.toCase() }
        }
        return cachedCases!!
    }

    fun getUniqueCase(completedCaseTitles: Set<String>, playerCount: Int, context: Context): Case? {
        val allCases = loadCases(context)
        val available = allCases.filter { it.title !in completedCaseTitles }
        val pool = if (available.isNotEmpty()) available else allCases
        val matchingCases = pool.filter { it.characters.size == playerCount }
        return if (matchingCases.isNotEmpty()) matchingCases.random(Random(System.currentTimeMillis()))
        else null
    }
}

// فئات مساعدة لتحويل JSON إلى كائنات Kotlin
@Serializable
data class JsonCharacter(
    val name: String,
    val age: Int,
    val occupation: String,
    val background: String,
    val traits: String,
    val hiddenMotive: String,
    val socialStatus: String,
    val relationshipToVictim: String,
    val relationshipToOtherSuspects: String,
    val relevantHistory: String
)

@Serializable
data class JsonCase(
    val title: String,
    val location: String,
    val time: String,
    val victim: String,
    val victimProfile: String,
    val description: String,
    val characters: List<JsonCharacter>,
    val evidenceList: List<String>,
    val suspicionDistribution: String,
    val hint: String,
    val explanation: String
)

fun JsonCase.toCase(): Case = Case(
    title = title,
    location = location,
    time = time,
    victim = victim,
    victimProfile = victimProfile,
    description = description,
    characters = characters.map { it.toCharacter() },
    evidenceList = evidenceList,
    suspicionDistribution = suspicionDistribution,
    hint = hint,
    explanation = explanation
)

fun JsonCharacter.toCharacter(): Character = Character(
    name = name,
    age = age,
    occupation = occupation,
    background = background,
    traits = traits,
    hiddenMotive = hiddenMotive,
    socialStatus = socialStatus,
    relationshipToVictim = relationshipToVictim,
    relationshipToOtherSuspects = relationshipToOtherSuspects,
    relevantHistory = relevantHistory
)