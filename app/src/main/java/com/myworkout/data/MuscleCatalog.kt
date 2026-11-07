package com.myworkout.data

object MuscleCatalog {
    fun combine(profiles: List<VisualProfile>): Map<Muscle, MuscleRole> = buildMap {
        profiles.forEach { profile -> profile.secondary.forEach { putIfAbsent(it, MuscleRole.SECONDARY) } }
        profiles.forEach { profile -> profile.primary.forEach { put(it, MuscleRole.PRIMARY) } }
    }
}
