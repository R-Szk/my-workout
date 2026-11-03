package com.myworkout.data

object SeedCatalog {
    val machines = listOf(
        Machine(1, "チェストプレス"),
        Machine(2, "ラットプルダウン"),
        Machine(3, "ショルダープレス"),
        Machine(4, "ラテラルレイズ"),
        Machine(5, "ディップ・チンアシスト", notes = "重量は補助重量。大きいほど補助が強い。"),
        Machine(6, "バーティカルインクラインベンチプレス"),
        Machine(7, "シーテッドロー"),
        Machine(8, "プリチャーカール"),
        Machine(9, "レッグプレス"),
    )
    val routines = listOf(Routine(1, "A：胸・肩・背中"), Routine(2, "B：胸・背中・腕・脚"))
    val exercises = listOf(
        RoutineExercise(1, 1, 0, 3, 10),
        RoutineExercise(1, 2, 1, 3, 10),
        RoutineExercise(1, 3, 2, 2, 10),
        RoutineExercise(1, 4, 3, 2, 12),
        RoutineExercise(1, 5, 4, 2, 10),
        RoutineExercise(2, 6, 0, 3, 10),
        RoutineExercise(2, 7, 1, 3, 10),
        RoutineExercise(2, 4, 2, 2, 12),
        RoutineExercise(2, 8, 3, 2, 10),
        RoutineExercise(2, 9, 4, 2, 10),
    )
}
