package com.myworkout.data

import androidx.room.*

@Entity(tableName = "machines")
data class Machine(
    @PrimaryKey val id: Long,
    val name: String,
    val imageAsset: String? = null,
    val notes: String = "",
)

@Entity(tableName = "routines")
data class Routine(@PrimaryKey val id: Long, val name: String)

@Entity(
    tableName = "routine_exercises",
    primaryKeys = ["routineId", "position"],
    foreignKeys = [
        ForeignKey(entity = Routine::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Machine::class, parentColumns = ["id"], childColumns = ["machineId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("machineId")],
)
data class RoutineExercise(
    val routineId: Long,
    val machineId: Long,
    val position: Int,
    val targetSets: Int,
    val targetReps: Int,
)

data class ExerciseWithMachine(
    @Embedded val exercise: RoutineExercise,
    @Relation(parentColumn = "machineId", entityColumn = "id") val machine: Machine,
)

// Room relation collections do not guarantee ordering; sort at the repository boundary.
data class RoutineWithExercises(
    @Embedded val routine: Routine,
    @Relation(entity = RoutineExercise::class, parentColumn = "id", entityColumn = "routineId")
    val exercises: List<ExerciseWithMachine>,
)
