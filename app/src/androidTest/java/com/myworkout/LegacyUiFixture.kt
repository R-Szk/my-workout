package com.myworkout

import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.withTransaction
import com.myworkout.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

/** Old catalog is installed only by regression tests, never by a fresh app launch. */
object LegacyUiFixture {
    fun ensure() = runBlocking {
        val db=AppDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        db.withTransaction {
            val dao=db.workoutDao()
            if(dao.observeMachines().first().none { it.id==1L }) {
                dao.insertMachines(SeedCatalog.machines)
                dao.insertRoutines(SeedCatalog.routines)
                dao.insertExercises(SeedCatalog.exercises)
            }
        }
    }
}
