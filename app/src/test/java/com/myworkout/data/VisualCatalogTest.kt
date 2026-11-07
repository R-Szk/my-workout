package com.myworkout.data

import org.junit.Assert.*
import org.junit.Test

class VisualCatalogTest {
    @Test fun fixedMachinesResolveWithoutDependingOnEditedNames() {
        val chest = SessionExercise(sessionId = 1, machineId = 1, machineName = "自分の名前", notes = "", position = 0, targetSets = 3, targetReps = 10)
        val profile = VisualCatalog.resolve(chest)!!
        assertEquals("machines/chest_press.png", profile.machineImage)
        assertEquals(setOf(Muscle.CHEST), profile.primary)
        assertEquals(setOf(Muscle.TRICEPS, Muscle.FRONT_DELTS), profile.secondary)
        assertNull(VisualCatalog.find("missing"))
        assertEquals(29, VisualCatalog.assets.size)
    }
    @Test fun combinedMachineUsesDifferentExerciseProfilesAndAmbiguousModelsNeedSelection() {
        val dip = VisualCatalog.find("assisted_dip")!!
        val chin = VisualCatalog.find("assisted_chin")!!
        assertEquals(dip.machineImage, chin.machineImage)
        assertNotEquals(dip.primary, chin.primary)
        assertEquals(LoadType.ASSISTANCE, dip.loadType)
        assertEquals(setOf(Muscle.LATS), VisualCatalog.find("lat_pulldown")!!.primary)
        assertTrue(VisualCatalog.find("seated_row_cable")!!.primary.contains(Muscle.MID_BACK))
        for (id in listOf(3L, 5L, 7L, 9L)) assertNull(VisualCatalog.defaultForMachine(id))
        assertEquals(2, VisualCatalog.options(5).size)
    }
    @Test fun mainMuscleWinsWhenCombiningRoles() {
        val muscles = MuscleCatalog.combine(listOf(VisualCatalog.find("chest_press")!!, VisualCatalog.find("shoulder_press_selectorized")!!))
        assertEquals(MuscleRole.PRIMARY, muscles[Muscle.FRONT_DELTS])
        assertEquals(MuscleRole.SECONDARY, muscles[Muscle.TRICEPS])
        assertNull(muscles[Muscle.QUADS])
    }
}
